package com.bwj.fintrack.account.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 적금 계좌 생성 요청
 * - 인증 미도입 상태라 userId를 직접 받는다.
 * - 초기 입금액은 최소 10,000원 이상 필수
 * - 자동이체는 필수 (매월 납입)
 * - 월 납입액과 납입일 필수
 */
public record CreateSavingsAccountRequest(

        @NotNull
        Long userId,

        @NotNull
        @DecimalMin("10000.00")
        @Digits(integer = 17, fraction = 2)
        BigDecimal initialDeposit,

        @NotNull
        @DecimalMin("10000.00")
        @Digits(integer = 17, fraction = 2)
        BigDecimal monthlyAmount,

        @NotNull
        @Min(1)
        @Max(28)
        Integer transferDay,  // 매월 납입일 (1~28일)

        @NotNull
        Long autoTransferId  // 자동이체 설정 ID
) {
}
