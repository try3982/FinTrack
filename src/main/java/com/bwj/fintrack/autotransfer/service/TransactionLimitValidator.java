package com.bwj.fintrack.autotransfer.service;

import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import com.bwj.fintrack.grade.GradeType;
import com.bwj.fintrack.transaction.repository.TransactionRepository;
import com.bwj.fintrack.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class TransactionLimitValidator {

    private final TransactionRepository transactionRepository;

    public void validateDailyLimit(User user,
                                   BigDecimal requestAmount) {

        GradeType grade = user.getGrade().getGradeType();
        BigDecimal dailyLimit = grade.getDailyLimit();

        // 무제한 등급은 패스
        if (dailyLimit == null) {
            return;
        }

        // 오늘 날짜 기준으로 사용자 출금/이체 아웃 합계 조회
        BigDecimal usedToday = transactionRepository.sumOutboundAmountForUserBetween(user.getId());
        if (usedToday == null) {
            usedToday = BigDecimal.ZERO;
        }

        BigDecimal after = usedToday.add(requestAmount);

        if (after.compareTo(dailyLimit) > 0) {
            throw new CustomException(ErrorCode.DAILY_LIMIT_EXCEEDED);
        }
    }
}
