package com.bwj.fintrack.account.service;
import com.bwj.fintrack.account.dto.request.CreateSavingsAccountRequest;
import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.entity.AccountType;
import com.bwj.fintrack.autotransfer.entity.AutoTransfer;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 계좌에 대한 공통 비즈니스 규칙 / 검증 로직 모음
 * - 소유자 확인
 * - 계좌 상태(Active 등) 확인
 * - 금액 검증 (양수 여부, 한도 등)
 * - 계좌번호 형식 검증
 *
 * 서비스 간(AccountService, TransactionService, TransactionHistoryService 등)에서
 * 중복 없이 재사용하기 위한 전담 컴포넌트.
 */
@Component
public class AccountValidator {

    // 시스템 정책 한도 (AccountService의 상수와 일치하도록 유지)
    private static final BigDecimal MAX_TX_AMOUNT = new BigDecimal("10000000.00");


    /**
     * 계좌가 해당 userId의 소유인지 검증
     * - account.getUser() == null 이거나
     * - userId 불일치면 예외
     */
    public void validateOwner(Account account, Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCOUNT_ACCESS);
        }
        if (account.getUser() == null ||
                !account.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCOUNT_ACCESS);
        }
    }



    /**
     * 계좌가 거래 가능한 상태인지(예: ACTIVE) 검증
     */
    public void validateActive(Account account) {
        if (!account.isActive()) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
    }

    /**
     * 금액이 양수인지, null이 아닌지 검증
     */
    public void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.AMOUNT_MUST_BE_POSITIVE);
        }
    }



    /**
     * 거래 한도 검증 (정책에 따라 필요 시 사용)
     * - 단일 거래가 너무 큰 경우 차단
     */
    public void validateMaxTxAmount(BigDecimal amount) {
        if (amount != null && amount.compareTo(MAX_TX_AMOUNT) > 0) {
            throw new CustomException(ErrorCode.AMOUNT_EXCEEDS_LIMIT);
        }
    }

    /**
     * 이 계좌가 "아무나 볼 수 있는 계좌"가 아니라
     * 실제 소유자(User)가 존재하는 계좌인지 확인한다.
     *
     * 현재는 소유자 존재 여부만 확인하고,
     * userId 일치 여부까지는 체크하지 않는다.
     *
     * 추후에는 (account.getUser().getId() == currentUserId) 비교가 필요하면
     * 별도 validateOwner(Account account, Long userId) 로 확장 가능하다.
     */
    public void validateOwnerPresent(Account account) {
        if (account.getUser() == null) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCOUNT_ACCESS);
        }
    }

    /**
     * 계좌번호 포맷 검증
     * - Account 엔티티의 정적 메서드를 활용
     */
    public void validateAccountNumberFormat(String accountNumber) {
        if (!Account.isValidAccountNo(accountNumber)) {
            throw new CustomException(ErrorCode.INVALID_ACCOUNT_NUMBER_FORMAT);
        }
    }

    /**
     * 인출/이체 전 잔액 충분한지 검사
     * - 인출 금액 > 현재 잔액이면 예외
     * - 정책 최소 잔액(MIN_BALANCE_VIOLATION 등)도 여기서 검증할 수 있음
     */
    public void validateWithdrawPossible(Account account, BigDecimal amount) {
        // 현재 잔액이 부족하면 예외
        if (account.getBalance().compareTo(amount) < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 계좌 유형별 최소 유지 잔액 정책 있는 경우
        // (Account 내부에 policyMinBalance 같은 필드가 있다고 가정)
        BigDecimal minBalancePolicy = account.getMinBalance();
        if (minBalancePolicy != null) {
            BigDecimal after = account.getBalance().subtract(amount);
            if (after.compareTo(minBalancePolicy) < 0) {
                throw new CustomException(ErrorCode.MIN_BALANCE_VIOLATION);
            }
        }
    }

    public void validateSavingsRequestPolicy(CreateSavingsAccountRequest request) {

        BigDecimal minRequired = BigDecimal
                .valueOf(AccountType.SAVINGS.getMinimumInitial()); // 예: 10000

        // 초기 입금 검증
        if (request.initialDeposit() == null
                || request.initialDeposit().compareTo(minRequired) < 0) {
            throw new CustomException(ErrorCode.INVALID_INITIAL_DEPOSIT_FOR_SAVINGS);
        }

        // 월 납입액 검증
        if (request.monthlyAmount() == null
                || request.monthlyAmount().compareTo(minRequired) < 0) {
            throw new CustomException(ErrorCode.INVALID_MONTHLY_AMOUNT);
        }

        // 자동이체 미설정 불가
        if (request.autoTransferId() == null) {
            throw new CustomException(ErrorCode.AUTO_TRANSFER_REQUIRED);
        }
    }

}
