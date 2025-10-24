package com.bwj.fintrack.account.service;

import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.entity.AccountStatus;
import com.bwj.fintrack.account.entity.AccountType;
import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import com.bwj.fintrack.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.text.DecimalFormat;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountNumberGenerator numberGenerator;

    private static final int MAX_ACCOUNT_GEN_RETRY = 3;
    private static final SecureRandom RANDOM = new SecureRandom();


    @Transactional
    public Account createActive(User user,
                                AccountType type,
                                BigDecimal initialBalance,
                                BigDecimal minBalance,
                                boolean autoTransfer) {

        // 1) 고유 계좌번호 확보
        String accountNumber = generateUniqueAccountNumber();

        // 2) 엔티티 생성(엔티티는 판단/생성만. 예외 없음)
        Account account = buildActiveAccount(user, accountNumber, type, initialBalance, minBalance, autoTransfer);

        // 3) 저장 (DB UNIQUE 충돌 시 내부에서 재시도)
        return persistWithUniqueGuard(account);
    }

    @Transactional
    public void deposit(Long accountId, BigDecimal amount) {
        Account account = getActiveAccountOrThrow(accountId);
        var reason = account.reasonDepositInvalid(amount);
        if (reason != null) {
            throw new CustomException(reason);
        }
        account.applyDeposit(amount);

    }

    @Transactional
    public void withdraw(Long accountId, BigDecimal amount) {
        Account account = getActiveAccountOrThrow(accountId);
        var reason = account.reasonWithdrawInvalid(amount);
        if (reason != null) {
            throw new CustomException(reason);
        }
        account.applyWithdraw(amount);
    }

    @Transactional
    public void close(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (account.getAccountStatus() == AccountStatus.CLOSED) {
            return;
        }
        account.setAccountStatus(AccountStatus.CLOSED);
        // closedAt 추후 추가 예정
    }

    @Transactional(readOnly = true)
    public boolean existsByAccountNumber(String accountNumber) {
        return accountRepository.existsByAccountNumber(accountNumber);
    }

    private String generateUniqueAccountNumber() {
        for (int i = 0; i < MAX_ACCOUNT_GEN_RETRY; i++) {
            String candidate = generateAccountNumber();
            if (!Account.isValidAccountNo(candidate)) continue;
            if (accountRepository.existsByAccountNumber(candidate)) continue;
            return candidate;
        }
        throw new CustomException(ErrorCode.DUPLICATE_ACCOUNT_NUMBER);
    }

    private Account buildActiveAccount(User user,
                                       String accountNumber,
                                       AccountType type,
                                       BigDecimal initialBalance,
                                       BigDecimal minBalance,
                                       boolean autoTransfer) {
        return Account.createActiveUnchecked(
                user, accountNumber, initialBalance, type, minBalance, autoTransfer
        );
    }

    private Account persistWithUniqueGuard(Account prototype) {
        for (int i = 0; i < MAX_ACCOUNT_GEN_RETRY; i++) {
            try {
                return accountRepository.save(prototype);
            } catch (DataIntegrityViolationException e) {
                // UNIQUE 충돌 시  새 번호로 재시도
                String newNo = generateUniqueAccountNumber();
                prototype = rebuildWithNewNumber(prototype, newNo);
            }
        }
        throw new CustomException(ErrorCode.DUPLICATE_ACCOUNT_NUMBER);
    }

    private Account rebuildWithNewNumber(Account old, String newAccountNumber) {
        return Account.createActiveUnchecked(
                old.getUser(),
                newAccountNumber,
                old.getBalance(),
                old.getAccountType(),
                old.getMinBalance(),
                old.getAutoTransfer()
        );
    }

    private Account getActiveAccountOrThrow(Long accountId) {
        Account a = accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (!a.isActive()) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        return a;
    }

    private String generateAccountNumber() {
        String part1 = format(RANDOM.nextInt(1000), "000");      // 3자리
        String part2 = format(RANDOM.nextInt(10000), "0000");    // 4자리
        String part3 = format(RANDOM.nextInt(10_000_000), "0000000"); // 7자리
        return part1 + "-" + part2 + "-" + part3;
    }


    private void validateMinInitial(AccountType type, BigDecimal initialDeposit) {
        if (initialDeposit == null) {
            throw new CustomException(ErrorCode.INITIAL_DEPOSIT_REQUIRED);
        }
        int min = type.getMinimumInitial();

        if (initialDeposit.compareTo(BigDecimal.valueOf(min)) < 0) {
            throw new CustomException(ErrorCode.INITIAL_DEPOSIT_BELOW_MIN);
        }
    }

    private static String format(int n, String pattern) {
        return new DecimalFormat(pattern).format(n);
    }

}
