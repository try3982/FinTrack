package com.bwj.fintrack.grade.service;



import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import com.bwj.fintrack.grade.GradeType;
import com.bwj.fintrack.grade.dto.response.GradePromotionResultResponse;
import com.bwj.fintrack.transaction.repository.TransactionRepository;
import com.bwj.fintrack.user.entity.User;
import com.bwj.fintrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GradePromotionService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * 회원 승급 평가 및 반영
     * - 강등은 없다: 올라갈 수 있으면 올리고, 아니면 유지
     */
    @Transactional
    public GradePromotionResultResponse evaluateAndPromote(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        GradeType current = user.getGrade().getGradeType();

        // 1) KPI 계산
        BigDecimal totalBalance = getTotalActiveBalance(userId); // 현재 활성 계좌 잔액 합
        BigDecimal recent30dVolume = getRecent30DaysVolume(userId); // 최근 30일 총 거래 금액

        // 2) 다음 등급이 가능한지 확인
        GradeType upgraded = decideNextGrade(current, totalBalance, recent30dVolume);

        // 3) 승급 가능하면 반영
        if (upgraded != current) {
            user.setGradeType(upgraded); // <-- User 엔티티에 setter 또는 promote 메서드 필요
            userRepository.save(user);
        }

        return GradePromotionResultResponse.from(
                current,
                upgraded,
                totalBalance,
                recent30dVolume
        );
    }

    private BigDecimal getTotalActiveBalance(Long userId) {
        BigDecimal sum = accountRepository.sumActiveBalanceByUserId(userId);
        return (sum != null) ? sum : BigDecimal.ZERO;
    }

    private BigDecimal getRecent30DaysVolume(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(30);
        BigDecimal sum = transactionRepository.sumTransactionAmountLast30Days(userId, from, now);
        return (sum != null) ? sum : BigDecimal.ZERO;
    }

    /**
     * 현재 등급과 KPI 기준으로 올라갈 수 있는 등급을 결정.
     * - 여러 단계 한 번에 올라갈 수도 있다 (예: 엄청 부자면 BRONZE -> GOLD)
     * - 강등은 하지 않는다.
     */
    private GradeType decideNextGrade(GradeType current,
                                      BigDecimal totalBalance,
                                      BigDecimal recent30dVolume) {

        // 브론즈 이상 레벨로 갈 조건부터 위로 쭉 검사해.
        // 높은 등급부터 맞으면 그걸로 준다.
        if (canBeDiamond(totalBalance, recent30dVolume)) {
            return max(current, GradeType.DIAMOND);
        }
        if (canBePremium(totalBalance, recent30dVolume)) {
            return max(current, GradeType.PREMIUM);
        }
        if (canBeGold(totalBalance, recent30dVolume)) {
            return max(current, GradeType.GOLD);
        }
        if (canBeSilver(totalBalance, recent30dVolume)) {
            return max(current, GradeType.SILVER);
        }
        return current;
    }

    // 승급 조건들
    private boolean canBeSilver(BigDecimal balance, BigDecimal volume) {
        return balance.compareTo(new BigDecimal("1000000.00")) >= 0   // 100만 이상 보유
                && volume.compareTo(new BigDecimal("3000000.00")) >= 0; // 30일 거래 300만 이상
    }

    private boolean canBeGold(BigDecimal balance, BigDecimal volume) {
        return balance.compareTo(new BigDecimal("5000000.00")) >= 0   // 500만 이상
                && volume.compareTo(new BigDecimal("10000000.00")) >= 0; // 30일 거래 1천만 이상
    }

    private boolean canBePremium(BigDecimal balance, BigDecimal volume) {
        return balance.compareTo(new BigDecimal("20000000.00")) >= 0   // 2천만 이상
                && volume.compareTo(new BigDecimal("30000000.00")) >= 0; // 30일 거래 3천만 이상
    }

    private boolean canBeDiamond(BigDecimal balance, BigDecimal volume) {
        return balance.compareTo(new BigDecimal("100000000.00")) >= 0   // 1억 이상
                && volume.compareTo(new BigDecimal("100000000.00")) >= 0; // 30일 거래 1억 이상
    }

    /**
     * 현재 등급 vs 목표 등급 중 더 높은 등급을 반환.
     * - 우리는 등급 순서를 직접 비교해줄 필요가 있다.
     */
    private GradeType max(GradeType a, GradeType b) {
        return (rank(b) > rank(a)) ? b : a;
    }

    /**
     * 등급 우선순위(높을수록 우대 등급)
     */
    private int rank(GradeType gradeType) {
        return switch (gradeType) {
            case BRONZE -> 1;
            case SILVER -> 2;
            case GOLD -> 3;
            case PREMIUM -> 4;
            case DIAMOND -> 5;
        };
    }
}
