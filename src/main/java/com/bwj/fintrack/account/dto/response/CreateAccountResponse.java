package com.bwj.fintrack.account.dto.response;

import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.entity.AccountStatus;

import java.math.BigDecimal;

public record CreateAccountResponse(
        String accountNumber,
        AccountStatus status,
        BigDecimal balance
) {
    public static CreateAccountResponse from(Account account) {
        return new CreateAccountResponse(
                account.getAccountNumber(),
                account.getAccountStatus(),
                account.getBalance()
        );
    }
}