package com.bwj.fintrack.autotransfer.service;


import com.bwj.fintrack.autotransfer.entity.AutoTransfer;
import com.bwj.fintrack.autotransfer.repository.AutoTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;


@Service
@RequiredArgsConstructor
public class AutoTransferSchedulerService {

    private final AutoTransferRepository autoTransferRepository;
    private final AutoTransferExecutionService autoTransferExecutionService;

    @Transactional
    public void runDueTransfers() {
        var now = LocalDateTime.now();

        // active == true && nextRunAt <= now 인 애들을 비관적 락으로 가져오는 쿼리
        var dueList = autoTransferRepository.findDueTransfersForUpdate(now);

        for (AutoTransfer schedule : dueList) {
            // 실행 시도 기록
            schedule.markAttempt(now);

            // 다시 한 번 현재 시점에서 실행 가능한지(멱등/중복 방지)
            if (!schedule.isDue(now)) {
                continue;
            }

            try {
                autoTransferExecutionService.executeAutoTransfer(schedule);
                // 성공 처리
                schedule.markSuccess(LocalDateTime.now());
            } catch (RuntimeException ex) {
                // 실패 처리
                schedule.markFailure();
                // 예외는 삼킨다. 개별 실패 때문에 전체 배치를 멈추지 않는다.
            }
        }
    }
}
