package com.bwj.fintrack.autotransfer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutoTransferSchedulerRunner {

    private final AutoTransferSchedulerService schedulerService;

    @Scheduled(fixedDelay = 60_000L) // 60초마다 한 번씩 돌린다
    public void tick() {
        schedulerService.runDueTransfers();
    }
}