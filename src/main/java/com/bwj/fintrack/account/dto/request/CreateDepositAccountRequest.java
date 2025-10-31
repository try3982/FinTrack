package com.bwj.fintrack.account.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 예금 계좌 생성 요청
 * - 인증 미도입 상태라 userId를 직접 받는다.
 * - 초기 입금액은 0원 이상 (선택사항)
 * - 자동이체는 선택사항 (false가 기본값)
 */
public record CreateDepositAccountRequest(

        @NotNull
        @Positive
        Long userId,

        @NotNull
        @PositiveOrZero
        @DecimalMin("0.00")
        @Digits(integer = 17, fraction = 2)
        BigDecimal initialDeposit,

        Boolean autoTransfer
) {
    // 기본값 처리를 위한 compact constructor
    public CreateDepositAccountRequest {
        if (autoTransfer == null) {
            autoTransfer = false;
        }
    }
}