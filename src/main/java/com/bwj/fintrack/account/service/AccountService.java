package com.bwj.fintrack.account.service;


import com.bwj.fintrack.account.dto.request.*;
import com.bwj.fintrack.account.dto.response.*;
import com.bwj.fintrack.account.service.factory.DepositAccountFactory;
//import com.bwj.fintrack.autotransfer.service.TransactionLimitValidator;
import com.bwj.fintrack.account.service.factory.SavingsAccountFactory;
import com.bwj.fintrack.grade.service.GradePromotionService;
import com.bwj.fintrack.transaction.command.DepositCommand;
import com.bwj.fintrack.transaction.command.TransactionExecutor;
import com.bwj.fintrack.transaction.command.WithdrawCommand;
import com.bwj.fintrack.transaction.dto.request.TransferRequest;
import com.bwj.fintrack.transaction.dto.request.WithdrawRequest;
import com.bwj.fintrack.transaction.dto.response.TransferResponse;
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

@Service
@RequiredArgsConstructor
public class AccountService {


    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountNumberGenerator numberGenerator;
    private final AccountValidator accountValidator;
   // private final TransactionLimitValidator transactionLimitValidator;
    private final GradePromotionService gradePromotionService;


    private final DepositAccountFactory depositAccountFactory;
    private final SavingsAccountFactory savingsAccountFactory;
    private final TransactionExecutor transactionExecutor;


    @Transactional
    public CreateDepositAccountResponse createDepositAccount(CreateDepositAccountRequest request) {

        // 1) 예금 계좌 도메인 객체 생성 (규칙은 팩토리가 책임)
        Account newAccount = depositAccountFactory.createAccount(request);

        // 2) 저장 (트랜잭션/영속화는 서비스 책임)
        Account saved = persistAccountOrThrowIfDuplicate(newAccount);

        // 3) 응답 DTO 변환
        return CreateDepositAccountResponse.from(saved);
    }

    @Transactional
    public CreateSavingsAccountResponse createSavingsAccount(CreateSavingsAccountRequest request) {

        // 1) 적금 계좌 엔티티 생성 (정책/검증은 팩토리 내부에서 수행)
        Account newAccount = savingsAccountFactory.createAccount(request);

        // 2) 저장 (중복 계좌번호 등 무결성 위반 시 예외 변환)
        Account saved = persistAccountOrThrowIfDuplicate(newAccount);

        // 3) 응답 DTO 구성
        //    - 적금은 월 납입 금액, 자동이체 정보, 이체일 등이 응답에 포함되는 구조였지?
        //      기존에 너가 `CreateSavingsAccountResponse.from(...)`에서
        //      monthlyAmount, transferDay, autoTransferId 등을 받도록 설계했어.
        return CreateSavingsAccountResponse.from(
                saved,
                request.monthlyAmount().setScale(2, RoundingMode.HALF_UP),
                request.transferDay(),
                request.autoTransferId()
        );
    }

   // 입금
    @Transactional
    public DepositResponse deposit(DepositRequest request) {

        DepositCommand command = new DepositCommand(
                request,
                accountRepository,
                transactionRepository,
                accountValidator
        );

        return transactionExecutor.execute(command);
    }

    /**
     * 출금
     */
    public WithdrawResponse withdraw(WithdrawRequest request) {

        WithdrawCommand command = new WithdrawCommand(
                request,
                accountRepository,
                transactionRepository,
                accountValidator
        );

        return transactionExecutor.execute(command);
    }

    /**
     * 이체
     */
    @Transactional
    public TransferResponse transfer(TransferRequest request) {

       // gradePromotionService.evaluateAndPromote(request.userId());

        if (request.fromAccountNumber().equals(request.toAccountNumber())) {
            throw new CustomException(ErrorCode.INVALID_AMOUNT);
        }

        accountValidator.validatePositiveAmount(request.amount());
        accountValidator.validateMaxTxAmount(request.amount());

        // 데드락 방지 순서대로 락 획득
        String a = request.fromAccountNumber();
        String b = request.toAccountNumber();
        final boolean swapped = a.compareTo(b) > 0;
        String first = swapped ? b : a;
        String second = swapped ? a : b;

        Account firstAcc = accountRepository.findWithLockByAccountNumber(first)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        Account secondAcc = accountRepository.findWithLockByAccountNumber(second)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        Account from = swapped ? secondAcc : firstAcc;
        Account to   = swapped ? firstAcc  : secondAcc;

        // 보내는 쪽: 소유자/상태 검증
        accountValidator.validateOwnerPresent(from);
        accountValidator.validateActive(from);

        // 받는 쪽: 상태만 확인
        accountValidator.validateActive(to);

        BigDecimal amount = normalizeAmount(request.amount());

        BigDecimal fee = calculateTransferFee(from, to);

        User owner = from.getUser();
        BigDecimal totalDebit = amount.add(fee);

        // 출금 가능 여부(잔액 등)
      //  transactionLimitValidator.validateDailyLimit(owner, totalDebit);
        accountValidator.validateWithdrawPossible(from, totalDebit);

        from.withdraw(totalDebit);
        to.deposit(amount);

        Transaction outTx = Transaction.transferOutSuccess(
                from,
                amount,
                request.methodType(),
                request.memo()
        );
        Transaction inTx  = Transaction.transferInSuccess(
                to,
                amount,
                request.methodType(),
                request.memo()
        );

        transactionRepository.save(outTx);
        transactionRepository.save(inTx);

        return TransferResponse.from(outTx, inTx);
    }

    /**
     * 단건 조회
     */
    @Transactional(readOnly = true)
    public AccountDetailResponse getAccountByNumber(String accountNumber) {

        // 포맷 검증을 validator로 이관
        accountValidator.validateAccountNumberFormat(accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 접근 가능한 계좌인지(현재는 "주인 없는 계좌는 금지")
        accountValidator.validateOwnerPresent(account);

        return AccountDetailResponse.from(account);
    }

    @Transactional
    public CloseAccountResponse closeAccount(CloseAccountRequest request) {
        if (request.userId() == null) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCOUNT_ACCESS);
        }

        Account account = accountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 본인 소유인지 (Close에서는 userId 비교까지 이미 하고 있으므로
        // 여기 로직은 그대로 유지해도 되고, 추후 validator에 옮길 수 있음)
        if (account.getUser() == null ||
                !account.getUser().getId().equals(request.userId())) {
            throw new CustomException(ErrorCode.ACCOUNT_CLOSE_FORBIDDEN);
        }

        account.closeAccount();

        Account saved = accountRepository.save(account);
        return CloseAccountResponse.from(saved);
    }

    private Account buildSavingsAccount(User user,
                                        String accountNo,
                                        CreateSavingsAccountRequest request) {

        BigDecimal normalizedInitial = toScale2(request.initialDeposit());
        BigDecimal normalizedMonthly = toScale2(request.monthlyAmount());

        return Account.createSavings(
                user,
                accountNo,
                normalizedInitial,
                normalizedMonthly
        );
    }

    @Transactional
    public RestoreAccountResponse restoreAccount(RestoreAccountRequest request) {
        if (request.userId() == null) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCOUNT_ACCESS);
        }

        Account account = accountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (account.getUser() == null ||
                !account.getUser().getId().equals(request.userId())) {
            throw new CustomException(ErrorCode.ACCOUNT_RESTORE_FORBIDDEN);
        }

        account.restoreAccount();

        Account saved = accountRepository.save(account);
        return RestoreAccountResponse.from(saved);
    }

    // --- 아래는 기존의 private 유틸들 그대로 유지 ---

    private void validateMinInitial(AccountType type, BigDecimal initialDeposit) {
        if (initialDeposit == null) {
            throw new CustomException(ErrorCode.INITIAL_DEPOSIT_REQUIRED);
        }
        int min = type.getMinimumInitial();
        if (initialDeposit.compareTo(BigDecimal.valueOf(min)) < 0) {
            throw new CustomException(ErrorCode.INITIAL_DEPOSIT_BELOW_MIN);
        }
    }

    private BigDecimal calculateTransferFee(Account from, Account to) {
        Long fromUserId = from.getUser().getId();
        Long toUserId   = to.getUser().getId();

        // 같은 사용자 소유 계좌 간 이체라면 수수료 없음
        if (fromUserId.equals(toUserId)) {
            return BigDecimal.ZERO;
        }

        // 타인에게 보내면 500원 부과
        return new BigDecimal("500.00");
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private void ensureAccountNoIsUnique(String accountNo) {
        if (accountRepository.existsByAccountNumber(accountNo)) {
            throw new CustomException(ErrorCode.DUPLICATE_ACCOUNT_NUMBER);
        }
    }

    private Account persistAccountOrThrowIfDuplicate(Account account) {
        try {
            return accountRepository.save(account);
        } catch (DataIntegrityViolationException e) {
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

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toScale2(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal computePolicyMinBalance(AccountType type) {
        return (type == AccountType.SAVINGS)
                ? new BigDecimal("10000.00")
                : null;
    }

    private Account buildAccount(
            User owner,
            String accountNumber,
            CreateAccountRequest req,
            BigDecimal initialDeposit,
            BigDecimal policyMinBalance
    ) {
        return Account.createActive(
                owner,
                accountNumber,
                initialDeposit,
                req.type(),
                policyMinBalance,
                false
        );
    }

    private Account saveAccount(Account account) {
        try {
            return accountRepository.save(account);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.DUPLICATE_ACCOUNT_NUMBER);
        }
    }
}
