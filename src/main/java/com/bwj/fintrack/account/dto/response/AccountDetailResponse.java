package com.bwj.fintrack.account.dto.response;

import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.entity.AccountStatus;
import com.bwj.fintrack.account.entity.AccountType;

import java.math.BigDecimal;

public record AccountDetailResponse(
        Long accountId,
        String accountNumber,
        BigDecimal balance,
        AccountStatus accountStatus,
        AccountType accountType,
        BigDecimal minBalance,
        Boolean autoTransfer
) {
    public static AccountDetailResponse from(Account a) {
        return new AccountDetailResponse(
                a.getId(),
                a.getAccountNumber(),
                a.getBalance(),
                a.getAccountStatus(),
                a.getAccountType(),
                a.getMinBalance(),
                a.getAutoTransfer()
        );
    }
}