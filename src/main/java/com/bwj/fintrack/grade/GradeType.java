package com.bwj.fintrack.grade;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public enum GradeType {

    BRONZE(new BigDecimal("0.90"), new BigDecimal("0.05"), new BigDecimal("0.01")),
    SILVER(new BigDecimal("0.80"), new BigDecimal("0.04"), new BigDecimal("0.02")),
    GOLD(new BigDecimal("0.70"), new BigDecimal("0.03"), new BigDecimal("0.03")),
    PREMIUM(new BigDecimal("0.60"), new BigDecimal("0.02"), new BigDecimal("0.04")),
    DIAMOND(new BigDecimal("0.50"), new BigDecimal("0.01"), new BigDecimal("0.05"));

    private final BigDecimal collateralRate;
    private final BigDecimal interestRate;
    private final BigDecimal discount;
}