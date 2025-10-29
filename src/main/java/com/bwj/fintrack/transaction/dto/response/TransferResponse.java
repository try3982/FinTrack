package com.bwj.fintrack.transaction.dto.response;

import com.bwj.fintrack.transaction.entity.Transaction;
import com.bwj.fintrack.transaction.entity.TransactionMethodType;
import com.bwj.fintrack.transaction.entity.TransactionResultType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferResponse(
        UUID outTransactionId,       // 보내는 쪽 거래 ID
        UUID inTransactionId,        // 받는 쪽 거래 ID (식별만)
        Long fromAccountId,          // 보내는 계좌 ID
        String toAccountNumber,      // 받는 계좌번호(필요시 마스킹 고려)
        BigDecimal amount,           // 이체 금액
        BigDecimal fromBalanceAfter, // 보내는 계좌의 이체 후 잔액 (본인 잔액만)
        TransactionResultType result,
        TransactionMethodType methodType,
        LocalDateTime transactedAt
) {
    public static TransferResponse from(Transaction outTx, Transaction inTx) {
        return new TransferResponse(
                outTx.getId(),
                inTx.getId(),
                outTx.getAccount().getId(),
                inTx.getAccount().getAccountNumber(),
                outTx.getAmount(),
                outTx.getBalanceSnapshot(),
                outTx.getTransactionResultType(),
                outTx.getTransactionMethodType(),
                outTx.getTransactionDate()
        );
    }
}