package com.bwj.fintrack.account.dto.response;

import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.entity.AccountStatus;

import java.time.LocalDateTime;

/**
 * 계좌 해지 결과 응답
 */
public record CloseAccountResponse(
        Long accountId,
        String accountNumber,
        AccountStatus status,
        LocalDateTime closedAt,
        LocalDateTime restoreUntil
) {
    public static CloseAccountResponse from(Account account) {
        return new CloseAccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountStatus(),
                account.getClosedAt(),
                account.getRestoreUntil()
        );
    }
}