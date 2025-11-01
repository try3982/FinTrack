package com.bwj.fintrack.account.service;
import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountNumberAssigner {

    private final AccountNumberGenerator numberGenerator;
    private final AccountRepository accountRepository;

    /**
     * 유효하고 중복 없는 계좌번호를 생성해 반환한다.
     * - 형식 검증
     * - 중복 검증
     */
    public String generateValidUniqueAccountNumber() {
        String candidate = numberGenerator.generateUnique();

        if (!Account.isValidAccountNo(candidate)) {
            throw new CustomException(ErrorCode.INVALID_ACCOUNT_NUMBER_FORMAT);
        }
        if (accountRepository.existsByAccountNumber(candidate)) {
            throw new CustomException(ErrorCode.DUPLICATE_ACCOUNT_NUMBER);
        }

        return candidate;
    }
}
