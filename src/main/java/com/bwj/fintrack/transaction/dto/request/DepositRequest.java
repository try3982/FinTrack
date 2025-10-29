package com.bwj.fintrack.transaction.dto.request;

import com.bwj.fintrack.transaction.entity.TransactionMethodType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record DepositRequest(

        @NotNull @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{7}$", message = "계좌번호 형식이 올바르지 않습니다.")
        String accountNumber,

        @DecimalMin(value = "0.01", message = "금액은 0보다 커야 합니다.")
        @Digits(integer = 17, fraction = 2)
        @NotNull
        BigDecimal amount,

        @NotNull
        TransactionMethodType methodType,

        @Size(max = 200) String memo

) { }