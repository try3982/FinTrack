package com.bwj.fintrack.account.service;

import com.bwj.fintrack.account.dto.request.CreateAccountRequest;
import com.bwj.fintrack.account.dto.response.CreateAccountResponse;
import com.bwj.fintrack.transaction.dto.request.WithdrawRequest;
import com.bwj.fintrack.transaction.dto.response.WithdrawResponse;
import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.entity.AccountType;
import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import com.bwj.fintrack.transaction.dto.request.DepositRequest;
import com.bwj.fintrack.transaction.dto.response.DepositResponse;
import com.bwj.fintrack.transaction.entity.Transaction;
import com.bwj.fintrack.transaction.entity.TransactionMethodType;
import com.bwj.fintrack.transaction.repository.TransactionRepository;
import com.bwj.fintrack.user.entity.User;
import com.bwj.fintrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.bwj.fintrack.common.exception.response.ErrorCode.INSUFFICIENT_BALANCE;
import static com.bwj.fintrack.common.exception.response.ErrorCode.MIN_BALANCE_VIOLATION;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final BigDecimal MAX_TX_AMOUNT = new BigDecimal("10000000.00");
    private static final BigDecimal MAX_BALANCE   = new BigDecimal("9999999999999.99");

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountNumberGenerator numberGenerator;


    @Transactional
    public CreateAccountResponse createAccount(CreateAccountRequest request) {

        validateMinInitial(request.type(), request.initialDeposit());

        User owner = getUserOrThrow(request.userId());

        String accountNo = generateAccountNo();

        ensureAccountNoIsUnique(accountNo);

        BigDecimal initial = toScale2(request.initialDeposit());

        BigDecimal policyMinBalance = computePolicyMinBalance(request.type());

        Account account = buildAccount(owner, accountNo, request, initial, policyMinBalance);

        Account saved = saveAccount(account);

        return CreateAccountResponse.from(saved);
    }


    /**
     * 입금 처리
     * - 금액 유효성 검증
     * - 계좌 조회(비관적 락)
     * - (임시) 소유자 검증 스텁
     * - 계좌 상태(Active) 검증
     * - 금액 스케일 정규화 후 잔액 증가
     * - 거래내역(입금) 생성/저장
     * - 응답 DTO 변환
     */
    @Transactional
    public DepositResponse deposit(DepositRequest request) {
        validateAmount(request.amount());

        Account account = accountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateAccountOwner(account);
        validateAccountActive(account);

        BigDecimal amount = normalizeAmount(request.amount());
        applyDeposit(account, amount);

        Transaction tx = Transaction.depositSuccess(account, amount, request.methodType(), request.memo());
        Transaction saved = transactionRepository.save(tx);

        return DepositResponse.from(saved);
    }

    @Transactional
    public WithdrawResponse withdraw(WithdrawRequest request) {
        // 1) 금액 검증
        validateAmount(request.amount());

        // 2) 계좌 조회 (입금과 동일하게 accountNumber 기반 조회)
        Account account = accountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 3) 권한/상태 검증 (입금과 동일)
        validateAccountOwner(account);
        validateAccountActive(account);

        // 4) 금액 스케일 정규화
        BigDecimal amount = normalizeAmount(request.amount());

        // 5) 도메인 적용 (입금과 동일하게 헬퍼 사용)
        applyWithdraw(account, amount);

        // 6) 거래내역 생성/저장 (입금과 대칭)
        Transaction tx = Transaction.withdrawalSuccess(
                account, amount, request.methodType(), request.memo()
        );
        Transaction saved = transactionRepository.save(tx);

        // 7) 응답 변환 (레코드의 from 사용)
        return WithdrawResponse.from(saved);
    }

    // 초기 입금 최소 금액 정책 검증
    private void validateMinInitial(AccountType type, BigDecimal initialDeposit) {
        if (initialDeposit == null) {
            throw new CustomException(ErrorCode.INITIAL_DEPOSIT_REQUIRED);
        }
        int min = type.getMinimumInitial();

        if (initialDeposit.compareTo(BigDecimal.valueOf(min)) < 0) {
            throw new CustomException(ErrorCode.INITIAL_DEPOSIT_BELOW_MIN);
        }
    }

    // 계좌번호 중복 여부 사전 검증
    private void ensureAccountNoIsUnique(String accountNo) {
        if (accountRepository.existsByAccountNumber(accountNo)) {
            throw new CustomException(ErrorCode.DUPLICATE_ACCOUNT_NUMBER);
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private String generateAccountNo() {
        String accountNumber = numberGenerator.generateUnique();
        if (!Account.isValidAccountNo(accountNumber)) {
            throw new CustomException(ErrorCode.INVALID_ACCOUNT_NUMBER_FORMAT);
        }
        return accountNumber;
    }

    // 금액 스케일을 소수점 둘째 자리로 통일(HALF_UP)
    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    //  값 스케일을 소수점 둘째 자리로 통일(HALF_UP)
    private BigDecimal toScale2(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 계좌유형에 따른 정책 최소 유지잔액 계산
     * - 예: 적금(SAVINGS)은 10,000원
     * - 그 외 null(제약 없음)
     */
    private BigDecimal computePolicyMinBalance(AccountType type) {
        return (type == AccountType.SAVINGS) ? new BigDecimal("10000.00") : null;
    }

    private Account buildAccount(
            User owner,
            String accountNumber,
            CreateAccountRequest req,
            BigDecimal initial,
            BigDecimal policyMinBalance
    ) {
        return Account.createActive(
                owner,
                accountNumber,
                initial,
                req.type(),
                policyMinBalance,
                false
        );
    }

    //Account 저장 (고유 제약 위반 시 DUPLICATE_ACCOUNT_NUMBER로 매핑)
    private Account saveAccount(Account account) {
        try {
            return accountRepository.save(account);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.DUPLICATE_ACCOUNT_NUMBER);
        }
    }

    // 금액 유효성 검증 (null 또는 0 이하 금지)
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.AMOUNT_MUST_BE_POSITIVE);
        }
    }

    // 계좌 조회(비관적 락) - 미존재 시 ACCOUNT_NOT_FOUND
    private Account getAccountOrThrowWithLock(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    /**
     * (임시) 계좌 소유자 검증 스텁
     * - 현재는 사용자 연계만 확인
     * - 추후 인증 도입 시 현재 사용자와 소유자 일치 여부를 검증하도록 교체
     */
    private void validateAccountOwner(Account account) {

        if (account.getUser() == null) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCOUNT_ACCESS);
        }
        // TODO: 인증 도입 후 현재 사용자와 소유자 일치 여부 검증 추가
    }

    //계좌 상태 검증 (Active가 아니면 예외)
    private void validateAccountActive(Account account) {
        if (!account.isActive()) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
    }

    // 잔액 증가 적용 (Account 엔티티의 deposit 사용)
    private void applyDeposit(Account account, BigDecimal amount) {
        account.deposit(amount);
    }

   // 거래 방법 기본값  (null이면 ONLINE)
    private TransactionMethodType resolveMethodType(TransactionMethodType methodType) {
        return (methodType != null) ? methodType : TransactionMethodType.ONLINE;
    }

    private void applyWithdraw(Account account, BigDecimal amount) {
        account.withdraw(amount);
    }
}
