package com.bwj.fintrack.account.dto.response;

import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.entity.AccountStatus;
import com.bwj.fintrack.account.entity.AccountType;

import java.math.BigDecimal;

/**
 * 예금 계좌 생성 응답
 */
public record CreateDepositAccountResponse(
        Long accountId,
        String accountNumber,
        AccountType accountType,
        AccountStatus accountStatus,
        BigDecimal balance,
        Boolean autoTransfer
) {
    public static CreateDepositAccountResponse from(Account account) {
        return new CreateDepositAccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getAccountStatus(),
                account.getBalance(),
                account.getAutoTransfer()
        );
    }
}