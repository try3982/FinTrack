package com.bwj.fintrack.account.dto.request;

import com.bwj.fintrack.account.entity.AccountType;
import jakarta.validation.constraints.*;


import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotNull @Positive
        Long userId,

        @NotNull
        AccountType type,

        @NotNull @PositiveOrZero @DecimalMin("0.00") @Digits(integer=17, fraction=2)
        BigDecimal initialDeposit
) {

}