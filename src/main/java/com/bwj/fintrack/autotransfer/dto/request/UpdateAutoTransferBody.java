package com.bwj.fintrack.autotransfer.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateAutoTransferBody(

        @NotNull
        Long userId,

        @NotNull
        @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{7}$", message = "계좌번호 형식이 올바르지 않습니다.")
        String toAccountNumber,

        @NotNull
        @DecimalMin(value = "0.01", message = "이체 금액은 0보다 커야 합니다.")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @NotNull
        @Min(value = 1, message = "dayOfMonth는 1 이상이어야 합니다.")
        @Max(value = 31, message = "dayOfMonth는 31 이하여야 합니다.")
        Integer dayOfMonth,

        @NotNull
        @Pattern(regexp = "^[0-2]\\d:[0-5]\\d$", message = "runTime 형식은 HH:mm 이어야 합니다.")
        String runTime,

        @NotNull
        Boolean active
) { }