package com.bwj.fintrack.account.service;

import com.bwj.fintrack.account.dto.request.CreateAccountRequest;
import com.bwj.fintrack.account.dto.response.CreateAccountResponse;
import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.entity.AccountType;
import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
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

    private void validateMinInitial(AccountType type, BigDecimal initialDeposit) {
        if (initialDeposit == null) {
            throw new CustomException(ErrorCode.INITIAL_DEPOSIT_REQUIRED);
        }
        int min = type.getMinimumInitial();

        if (initialDeposit.compareTo(BigDecimal.valueOf(min)) < 0) {
            throw new CustomException(ErrorCode.INITIAL_DEPOSIT_BELOW_MIN);
        }
    }

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

    private BigDecimal toScale2(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

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

    private Account saveAccount(Account account) {
        try {
            return accountRepository.save(account);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.DUPLICATE_ACCOUNT_NUMBER);
        }
    }
}
