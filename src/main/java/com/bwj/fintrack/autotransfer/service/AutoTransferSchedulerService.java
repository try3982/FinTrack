package com.bwj.fintrack.autotransfer.service;

import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.account.service.AccountValidator;
import com.bwj.fintrack.autotransfer.command.ScheduledTransferCommand;
import com.bwj.fintrack.autotransfer.entity.AutoTransfer;
import com.bwj.fintrack.autotransfer.repository.AutoTransferRepository;
import com.bwj.fintrack.transaction.command.TransactionExecutor;
import com.bwj.fintrack.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 스케줄러가 주기적으로 호출하는 자동이체 실행 서비스.
 *
 * 변경된 점:
 *  - 실제 돈 이동은 ScheduledTransferCommand에 캡슐화된다.
 *  - 여기서는 커맨드를 조립해서 TransactionExecutor.execute()에 넘기는 오케스트레이터 역할만 수행한다.
 *  - 성공/실패에 맞춰 스케줄러 엔티티의 상태를 전이(markSuccess/markFailure)한다.
 */
@Service
@RequiredArgsConstructor
public class AutoTransferSchedulerService {

    private final AutoTransferRepository autoTransferRepository;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountValidator accountValidator;
    private final TransactionLimitValidator transactionLimitValidator;
    private final TransactionExecutor transactionExecutor;

    /**
     * 실행 시점(now) 기준으로 nextRunAt이 도래한(active && nextRunAt <= now) 자동이체들을
     * 순회하며 커맨드 패턴으로 실행한다.
     *
     * 각 자동이체별로 독립 트랜잭션이 보장되도록 @Transactional은 메서드 전체에 걸고,
     * 커맨드 내부에서도 TransactionExecutor가 @Transactional 경계를 잡아준다.
     *
     * 정책: 하나 실패했다고 전체 배치를 중단하지 않는다.
     */
    @Transactional
    public void runDueTransfers() {

        LocalDateTime now = LocalDateTime.now();

        // 대상 스케줄을 비관적 락으로 확보
        var dueList = autoTransferRepository.findDueTransfersForUpdate(now);

        for (AutoTransfer schedule : dueList) {

            // 시도 기록
            schedule.markAttempt(now);

            // 멱등/중복방지: 현재 시점에서 여전히 실행해야 하는지 다시 한번 점검
            if (!schedule.isDue(now)) {
                continue;
            }

            try {
                // 1) 자동이체 1건 -> ScheduledTransferCommand로 래핑
                ScheduledTransferCommand command = new ScheduledTransferCommand(
                        schedule,
                        accountRepository,
                        transactionRepository,
                        accountValidator,
                        transactionLimitValidator
                );

                // 2) executor 통해 실제 금전 이동 실행 (@Transactional 보장)
                transactionExecutor.execute(command);

                // 3) 성공 상태 반영 (nextRunAt 갱신, failCount 초기화 등)
                schedule.markSuccess(LocalDateTime.now());

            } catch (RuntimeException ex) {
                // 실패 처리 (failCount++, 재시도 초과 시 active=false 가능)
                schedule.markFailure();

                // 로깅 등은 여기서 하면 된다. (필요하면 Logger 주입)
                // ex.printStackTrace();
            }
        }
    }
}
