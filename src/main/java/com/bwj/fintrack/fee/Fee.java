package com.bwj.fintrack.fee;


import com.bwj.fintrack.grade.GradeType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(
        name = "f_fees",
        uniqueConstraints = @UniqueConstraint(name = "ux_fees_grade", columnNames = "grade_type")
)
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Fee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_type", nullable = false, length = 16)
    private GradeType gradeType;

    @Column(precision = 10, scale = 2)
    private BigDecimal collateralRate;

    @Column(precision = 10, scale = 2)
    private BigDecimal interestRate;

    @Column(precision = 10, scale = 2)
    private BigDecimal discount;

    @Builder
    private Fee(Long id, GradeType gradeType, BigDecimal collateralRate, BigDecimal interestRate, BigDecimal discount) {
        this.id = id;
        this.gradeType = gradeType;
        this.collateralRate = collateralRate;
        this.interestRate = interestRate;
        this.discount = discount;
    }
}
