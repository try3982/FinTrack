package com.bwj.fintrack.autotransfer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoTransferRunner {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final AutoTransferService autoService;
    private final AutoTransferRepository autoRepo;
    private final TransactionService txService; // fromNo -> toNo 금액 이체

    @Scheduled(fixedDelay = 60_000) // 1분 간격
    @Transactional
    public void runDue() {
        var now = LocalDateTime.now();
        var dueList = autoService.findDue(now);
        for (AutoTransfer at : dueList) {
            try {
                // 실제 이체 실행 (동시성: TransactionService 내부에서 비관적락/멱등 처리 권장)
                String fromNo = at.getFromAccount().getAccountNumber();
                String toNo = at.getToAccountNo();
                txService.transfer(fromNo, toNo, at.getAmount(), "[AUTO] monthly");

                at.markSuccess(now, ZONE);
                autoRepo.save(at);
            } catch (Exception e) {
                log.warn("AutoTransfer failed id={}, msg={}", at.getId(), e.getMessage());
                at.markFailure();
                if (at.getFailCount() >= at.getMaxRetries()) {
                    // 재시도 초과 시 비활성화 등 정책
                    // at.setActive(false);  // 세터가 없다면 재생성 패턴 적용
                }
                autoRepo.save(at);
            }
        }
    }
}