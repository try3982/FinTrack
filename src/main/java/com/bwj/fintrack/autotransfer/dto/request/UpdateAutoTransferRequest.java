package com.bwj.fintrack.autotransfer.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 자동이체 수정 요청
 * - 현재 인증 미도입이라 userId도 받는다.
 * - 수정 가능한 필드만 포함.
 */
public record UpdateAutoTransferRequest(

        @NotNull
        Long userId,

        @NotNull
        Long autoTransferId,

        // 수취 계좌 변경 허용 (선택적)
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

        /**
         * 실행 시각. "HH:mm" 형식 가정 (예: "09:00")
         * 검증은 서비스에서 한 번 더 할 수 있음.
         */
        @NotNull
        @Pattern(regexp = "^[0-2]\\d:[0-5]\\d$", message = "runTime 형식은 HH:mm 이어야 합니다.")
        String runTime,

        @NotNull
        Boolean active
) { }