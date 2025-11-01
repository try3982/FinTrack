package com.bwj.fintrack.account.service.factory;

import com.bwj.fintrack.account.dto.request.CreateSavingsAccountRequest;
import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.account.service.AccountNumberAssigner;
import com.bwj.fintrack.account.service.AccountNumberGenerator;
import com.bwj.fintrack.account.service.AccountValidator;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import com.bwj.fintrack.user.entity.User;
import com.bwj.fintrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 적금(SAVINGS) 계좌 생성을 전담하는 팩토리.
 *
 * 책임:
 * - 사용자 조회
 * - 정책 검증 (AccountValidator에 위임)
 * - 계좌번호 생성 및 유일성 보장
 * - 금액 스케일 정규화
 * - Account.createSavings(...) 호출
 *
 * 저장은 Service에서 담당.
 */
@Component
@RequiredArgsConstructor
public class SavingsAccountFactory implements AccountFactory<CreateSavingsAccountRequest> {

    private final UserRepository userRepository;
    private final AccountValidator accountValidator;
    private final AccountNumberAssigner accountNumberAssigner;

    @Override
    public Account createAccount(CreateSavingsAccountRequest request) {

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 적금 정책 검증 (최소 예치금, 자동이체 등)
        accountValidator.validateSavingsRequestPolicy(request);

        // 계좌번호 발급 (형식, 중복불가 포함 보장)
        String accountNo = accountNumberAssigner.generateValidUniqueAccountNumber();

        BigDecimal normalizedInitial  = request.initialDeposit().setScale(2, RoundingMode.HALF_UP);
        BigDecimal normalizedMonthly = request.monthlyAmount().setScale(2, RoundingMode.HALF_UP);

        return Account.createSavings(
                user,
                accountNo,
                normalizedInitial,
                normalizedMonthly
        );
    }
}
