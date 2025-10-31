package com.bwj.fintrack.account.dto.response;

import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.entity.AccountStatus;
import com.bwj.fintrack.account.entity.AccountType;

import java.math.BigDecimal;

/**
 * 계좌 활성화 응답
 */
public record RestoreAccountResponse(
        Long accountId,
        String accountNumber,
        AccountStatus accountStatus,
        AccountType accountType,
        BigDecimal balance,
        String message
) {
    public static RestoreAccountResponse from(Account account) {
        return new RestoreAccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountStatus(),
                account.getAccountType(),
                account.getBalance(),
                "계좌가 정상적으로 활성화되었습니다."
        );
    }
}