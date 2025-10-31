package com.bwj.fintrack.account.dto.response;

import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.entity.AccountStatus;
import com.bwj.fintrack.account.entity.AccountType;

import java.math.BigDecimal;

/**
 * 적금 계좌 생성 응답
 */
public record CreateSavingsAccountResponse(
        Long accountId,
        String accountNumber,
        AccountType accountType,
        AccountStatus accountStatus,
        BigDecimal balance,
        BigDecimal monthlyAmount,
        Integer transferDay,
        Long autoTransferId
) {
    public static CreateSavingsAccountResponse from(
            Account account,
            BigDecimal monthlyAmount,
            Integer transferDay,
            Long autoTransferId
    ) {
        return new CreateSavingsAccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getAccountStatus(),
                account.getBalance(),
                monthlyAmount,
                transferDay,
                autoTransferId
        );
    }
}