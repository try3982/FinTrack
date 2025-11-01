package com.bwj.fintrack.grade.dto.response;

import com.bwj.fintrack.grade.GradeType;
import java.math.BigDecimal;

public record GradePromotionResultResponse(
        GradeType beforeGrade,
        GradeType afterGrade,
        BigDecimal totalActiveBalance,
        BigDecimal recent30dVolume
) {
    public static GradePromotionResultResponse from(
            GradeType beforeGrade,
            GradeType afterGrade,
            BigDecimal totalActiveBalance,
            BigDecimal recent30dVolume
    ) {
        return new GradePromotionResultResponse(
                beforeGrade,
                afterGrade,
                totalActiveBalance,
                recent30dVolume
        );
    }
}
