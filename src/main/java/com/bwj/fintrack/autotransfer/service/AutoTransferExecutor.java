package com.bwj.fintrack.autotransfer.service;



import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.autotransfer.entity.AutoTransfer;
import com.bwj.fintrack.autotransfer.repository.AutoTransferRepository;
import com.bwj.fintrack.transaction.entity.Transaction;
import com.bwj.fintrack.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoTransferExecutor {

    private final AutoTransferRepository autoTransferRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * 일정 주기로 실행 대상 자동이체를 꺼내 처리한다.
     *
     * - isDue(now) 로 실제 조건 재확인 (active, nextRunAt, failCount)
     * - 출금 계좌에서 withdraw() 시도
     * - 입금 계좌 deposit()
     * - Transaction 로그 남김 (TRANSFER_OUT / TRANSFER_IN)
     * - 성공 시 markSuccessAndScheduleNext()
     * - 실패 시 markFailure()
     */
    @Scheduled(fixedDelay = 60000) // 예: 60초마다 체크
    @Transactional
    public void executeDueAutoTransfers() {

        var now = java.time.LocalDateTime.now();

        // 우선 후보 전부 가져와서
        List<AutoTransfer> dueList =
                autoTransferRepository.findByActiveIsTrueAndNextRunAtLessThanEqual(now);

        for (AutoTransfer rule : dueList) {

            // 멱등/재시도 제어: 같은 rule이 동일 트랜잭션 안에서 2번 처리 안 되도록 일단 markAttempt
            rule.markAttempt(now);

            // rule.isDue(now) 재확인 (failCount 초과 등)
            if (!rule.isDue(now)) {
                continue;
            }

            boolean success = tryExecuteOneRule(rule);

            if (success) {
                rule.markSuccessAndScheduleNext(); // nextRunAt 다음 달로 밀고 failCount=0
            } else {
                rule.markFailure(); // failCount++, 필요하면 active=false
            }
        }
    }

    /**
     * 단일 자동이체 규칙 실행
     * 출금 계좌 -> 입금 계좌
     * Transaction 로그 2건 기록
     * 실패시 false 반환
     */
    private boolean tryExecuteOneRule(AutoTransfer rule) {
        // 출금 계좌
        Account from = accountRepository.findById(rule.getFromAccount().getId())
                .orElse(null);
        // 입금 계좌
        Account to = accountRepository.findByAccountNumber(rule.getToAccountNo())
                .orElse(null);

        // 계좌 유효성/활성 체크
        if (from == null || to == null) return false;
        if (!from.isActive() || !to.isActive()) return false;

        var amount = rule.getAmount();

        // 출금 시도
        try {
            from.withdraw(amount);
        } catch (RuntimeException ex) {
            // 잔액 부족, 최소잔액 위반 등
            return false;
        }

        // 입금
        to.deposit(amount);

        // 거래내역 기록
        Transaction outTx = Transaction.transferOutSuccess(
                from,
                amount,
                null,
                "[AUTO] 정기이체"
        );
        Transaction inTx = Transaction.transferInSuccess(
                to,
                amount,
                null,
                "[AUTO] 정기이체"
        );

        transactionRepository.save(outTx);
        transactionRepository.save(inTx);

        return true;
    }
}
