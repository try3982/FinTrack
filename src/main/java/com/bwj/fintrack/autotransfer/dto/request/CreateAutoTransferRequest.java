package com.bwj.fintrack.autotransfer.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 자동이체 규칙 생성 요청 DTO
 * - 매달 dayOfMonth/runTime 에 fromAccount -> toAccountNo 로 amount 이체
 */
public record CreateAutoTransferRequest(

        @NotNull
        Long userId, // 인증 붙기 전까지는 바디로 받음. 나중엔 SecurityContext로 교체 예정.

        @NotNull
        @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{7}$", message = "출금 계좌번호 형식이 올바르지 않습니다.")
        String fromAccountNumber,

        @NotNull
        @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{7}$", message = "입금 계좌번호 형식이 올바르지 않습니다.")
        String toAccountNumber,

        @NotNull
        @DecimalMin(value = "0.01", message = "금액은 0보다 커야 합니다.")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @NotNull
        @Min(1) @Max(31)
        Integer dayOfMonth,

        // "HH:mm" 형태로 받는다. 예: "09:30"
        @NotBlank
        @Pattern(regexp = "^[0-2]\\d:[0-5]\\d$", message = "runTime은 HH:mm 형식이어야 합니다.")
        String runTime
) { }