package com.bwj.fintrack.autotransfer.dto.response;

import com.bwj.fintrack.autotransfer.entity.AutoTransfer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateAutoTransferResponse(
        Long autoTransferId,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        Integer dayOfMonth,
        String runTime,
        LocalDateTime nextRunAt,
        boolean active,
        int failCount,
        int maxRetries
) {
    public static CreateAutoTransferResponse from(AutoTransfer at) {
        return new CreateAutoTransferResponse(
                at.getId(),
                at.getFromAccount().getAccountNumber(),
                at.getToAccountNo(),
                at.getAmount(),
                at.getDayOfMonth(),
                at.getRunTime(),
                at.getNextRunAt(),
                at.isActive(),
                at.getFailCount(),
                at.getMaxRetries()
        );
    }
}