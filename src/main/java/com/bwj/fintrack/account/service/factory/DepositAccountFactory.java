package com.bwj.fintrack.account.service.factory;

import com.bwj.fintrack.account.dto.request.CreateDepositAccountRequest;
import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.service.AccountNumberAssigner;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import com.bwj.fintrack.user.entity.User;
import com.bwj.fintrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 예금(DEPOSIT) 계좌 생성을 전담하는 팩토리.
 *
 * 책임:
 * - 사용자 조회
 * - 유효하고 중복 없는 계좌번호 확보 (AccountNumberAssigner)
 * - 초기 예치금 scale 맞추기
 * - Account.createDeposit(...) 호출하여 예금 계좌 엔티티 생성
 *
 * 저장은 AccountService에서 담당한다.
 */
@Component
@RequiredArgsConstructor
public class DepositAccountFactory implements AccountFactory<CreateDepositAccountRequest> {

    private final UserRepository userRepository;
    private final AccountNumberAssigner accountNumberAssigner;

    @Override
    public Account createAccount(CreateDepositAccountRequest request) {

        // 1) 사용자 조회
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2) 계좌번호 확보 (형식 유효성 + 중복 보장 포함)
        String accountNo = accountNumberAssigner.generateValidUniqueAccountNumber();

        // 3) 초기 입금액 정규화 (scale = 2 강제)
        BigDecimal normalizedInitial = normalizeScale2(request.initialDeposit());

        // 4) 예금 계좌 엔티티 생성
        //    - Account.createDeposit(...) 내부에서 예금 전용 도메인 규칙(초기 잔액 >= 0 등)을 보장
        return Account.createDeposit(
                user,
                accountNo,
                normalizedInitial,
                request.autoTransfer()
        );
    }

    private BigDecimal normalizeScale2(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
