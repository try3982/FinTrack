package com.bwj.fintrack.transaction.dto.response;

import com.bwj.fintrack.transaction.entity.Transaction;
import com.bwj.fintrack.transaction.entity.TransactionMethodType;
import com.bwj.fintrack.transaction.entity.TransactionResultType;
import com.bwj.fintrack.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionHistoryItemResponse(
        UUID transactionId,
        TransactionType type,
        TransactionResultType result,
        BigDecimal amount,
        BigDecimal balanceAfter,      // 계좌 잔액 스냅샷 (내 계좌 본인 기준)
        TransactionMethodType methodType,
        LocalDateTime transactedAt
) {
    public static TransactionHistoryItemResponse from(Transaction tx) {
        return new TransactionHistoryItemResponse(
                tx.getId(),
                tx.getTransactionType(),
                tx.getTransactionResultType(),
                tx.getAmount(),
                tx.getBalanceSnapshot(),
                tx.getTransactionMethodType(),
                tx.getTransactionDate()
        );
    }
}