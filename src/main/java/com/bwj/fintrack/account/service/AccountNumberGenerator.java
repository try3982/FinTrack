package com.bwj.fintrack.account.service;

import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private final SecureRandom random = new SecureRandom();
    private final AccountRepository accountRepository;


    public String generateUnique() {
        for (int i = 0; i < 20; i++) {
            String candidate = generateOnce();
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new CustomException(ErrorCode.ACCOUNT_NUMBER_GENERATION_FAILED);
    }

    private String generateOnce() {
        // ###-####-#######
        return String.format("%03d-%04d-%07d",
                random.nextInt(1000),
                random.nextInt(10000),
                random.nextInt(10_000_000));
    }

}
