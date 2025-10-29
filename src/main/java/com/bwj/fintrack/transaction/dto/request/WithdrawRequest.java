package com.bwj.fintrack.transaction.dto.request;

import com.bwj.fintrack.transaction.entity.TransactionMethodType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record WithdrawRequest(
        @NotNull
        @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{7}$", message = "계좌번호 형식이 올바르지 않습니다.")
        String accountNumber,

        @NotNull
        @DecimalMin(value = "0.01", message = "금액은 0보다 커야 합니다.")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @NotNull
        TransactionMethodType methodType,

        String memo
) { }