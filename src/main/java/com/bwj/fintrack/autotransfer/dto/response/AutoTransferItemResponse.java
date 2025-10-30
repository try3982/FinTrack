package com.bwj.fintrack.autotransfer.dto.response;


import com.bwj.fintrack.autotransfer.entity.AutoTransfer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 자동이체 규칙 한 건에 대한 정보.
 * 프론트에서 카드 형태로 보여줄 수 있는 단위.
 */
public record AutoTransferItemResponse(
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
    public static AutoTransferItemResponse from(AutoTransfer at) {
        return new AutoTransferItemResponse(
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