package com.bwj.fintrack.grade;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public enum GradeType {

    BRONZE(
            new BigDecimal("0.90"), // collateralRate
            new BigDecimal("0.05"), // interestRate
            new BigDecimal("0.01"), // discount
            new BigDecimal("1000000.00") // dailyLimit (1,000,000원)
    ),
    SILVER(
            new BigDecimal("0.80"),
            new BigDecimal("0.04"),
            new BigDecimal("0.02"),
            new BigDecimal("5000000.00") // 5,000,000원
    ),
    GOLD(
            new BigDecimal("0.70"),
            new BigDecimal("0.03"),
            new BigDecimal("0.03"),
            new BigDecimal("10000000.00") // 10,000,000원
    ),
    PREMIUM(
            new BigDecimal("0.60"),
            new BigDecimal("0.02"),
            new BigDecimal("0.04"),
            null // 무제한
    ),
    DIAMOND(
            new BigDecimal("0.50"),
            new BigDecimal("0.01"),
            new BigDecimal("0.05"),
            null
    );

    private final BigDecimal collateralRate;
    private final BigDecimal interestRate;
    private final BigDecimal discount;
    private final BigDecimal dailyLimit;
}
