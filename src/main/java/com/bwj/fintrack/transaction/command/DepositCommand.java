package com.bwj.fintrack.transaction.command;

import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.service.AccountValidator;
import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import com.bwj.fintrack.transaction.dto.request.DepositRequest;
import com.bwj.fintrack.transaction.dto.response.DepositResponse;
import com.bwj.fintrack.transaction.entity.Transaction;
import com.bwj.fintrack.transaction.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 입금(Deposit) 거래를 표현하는 커맨드.
 *
 * 책임:
 *  - 요청 검증 (금액 유효성, 한도)
 *  - 계좌 조회 & 상태 검증
 *  - 도메인 동작(account.deposit)
 *  - Transaction 엔티티 생성 및 저장
 *  - 최종 응답 DTO 생성
 *
 * 이 커맨드는 "입금이라는 거래 1건" 자체를 의미한다.
 */
public class DepositCommand implements TransactionCommand<DepositResponse> {

    private final DepositRequest request;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountValidator accountValidator;

    public DepositCommand(DepositRequest request,
                          AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          AccountValidator accountValidator) {

        this.request = request;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountValidator = accountValidator;
    }

    @Override
    public DepositResponse execute() {

        // 금액 검증
        accountValidator.validatePositiveAmount(request.amount());
        accountValidator.validateMaxTxAmount(request.amount());

        // 계좌 가져오고 비관적 락
        Account account = accountRepository.findWithLockByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 계좌 상태 검증
        accountValidator.validateOwnerPresent(account);
        accountValidator.validateActive(account);

        // 금액 스케일 정규화
        BigDecimal amount = normalizeScale2(request.amount());

        // 도메인에 위임 (Account.deposit)
        account.deposit(amount);

        // 거래 기록 엔티티 생성
        Transaction tx = Transaction.depositSuccess(
                account,
                amount,
                request.methodType(),
                request.memo()
        );

        Transaction savedTx = transactionRepository.save(tx);

        // 응답 DTO
        return DepositResponse.from(savedTx);
    }

    private BigDecimal normalizeScale2(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
