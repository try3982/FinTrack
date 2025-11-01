package com.bwj.fintrack.grade;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public enum GradeType {

    BRONZE(
            new BigDecimal("0.70"), // collateralRate: 담보의 70%까지 대출 한도 인정
            new BigDecimal("0.050"), // interestRate: 5.0% (기본 이자율 / 대출 이자율 등)
            new BigDecimal("0.00"),  // discount: 수수료 할인율 (0% 할인)
            new BigDecimal("1000000.00") // dailyLimit: 하루 1,000,000원
    ),
    SILVER(
            new BigDecimal("0.80"),
            new BigDecimal("0.040"), // 4.0%
            new BigDecimal("0.10"),  // 10% 수수료 할인
            new BigDecimal("5000000.00")
    ),
    GOLD(
            new BigDecimal("0.90"),
            new BigDecimal("0.030"), // 3.0%
            new BigDecimal("0.20"),  // 20% 수수료 할인
            new BigDecimal("10000000.00")
    ),
    PREMIUM(
            new BigDecimal("0.95"),
            new BigDecimal("0.020"), // 2.0%
            new BigDecimal("0.30"),  // 30% 수수료 할인
            null // 무제한
    ),
    DIAMOND(
            new BigDecimal("1.00"),
            new BigDecimal("0.010"), // 1.0%
            new BigDecimal("0.50"),  // 50% 수수료 할인
            null
    );

    /**
     * 담보 인정 비율 (LTV 비슷한 개념)
     * 예: 0.70이면 담보 100만원 맡겼을 때 70만원까지 대출 한도 인정
     */
    private final BigDecimal collateralRate;

    /**
     * 등급별 기준 이자율 (연 이율)
     * 예금/적금 우대 이율이 될 수도 있고,
     * 대출 시 적용되는 base rate가 될 수도 있다.
     */
    private final BigDecimal interestRate;

    /**
     * 수수료 할인율
     * 예: 0.30 → 30% 할인
     * (기본 수수료가 500원이면 500 * (1 - 0.30) = 350원)
     */
    private final BigDecimal discount;

    /**
     * 등급별 하루 이체/출금 한도.
     * null이면 무제한.
     */
    private final BigDecimal dailyLimit;
}
