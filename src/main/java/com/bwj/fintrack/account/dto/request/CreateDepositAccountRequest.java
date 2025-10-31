package com.bwj.fintrack.account.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 예금 계좌 생성 요청
 * - 인증 미도입 상태라 userId를 직접 받는다.
 * - 초기 입금액은 0원 가능 (null이면 0원으로 처리)
 * - 자동이체는 선택사항 (false가 기본값)
 */
public record CreateDepositAccountRequest(

        @NotNull
        Long userId,

        @PositiveOrZero
        @DecimalMin("0.00")
        @Digits(integer = 17, fraction = 2)
        BigDecimal initialDeposit,

        Boolean autoTransfer
) {

    public CreateDepositAccountRequest {
        if (initialDeposit == null) {
            initialDeposit = BigDecimal.ZERO;
        }
        if (autoTransfer == null) {
            autoTransfer = false;
        }
    }
}