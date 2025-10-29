package com.bwj.fintrack.transaction.dto.response;

import com.bwj.fintrack.transaction.entity.Transaction;
import com.bwj.fintrack.transaction.entity.TransactionMethodType;
import com.bwj.fintrack.transaction.entity.TransactionResultType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WithdrawResponse(
        UUID transactionId,
        Long accountId,
        BigDecimal amount,
        BigDecimal balanceAfter,
        TransactionResultType result,
        TransactionMethodType methodType,
        LocalDateTime transactedAt
) {
    public static WithdrawResponse from(Transaction transaction) {
        return new WithdrawResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getAmount(),
                transaction.getBalanceSnapshot(),
                transaction.getTransactionResultType(),
                transaction.getTransactionMethodType(),
                transaction.getTransactionDate()
        );
    }
}