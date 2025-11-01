package com.bwj.fintrack.transaction.command;

import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.account.service.AccountValidator;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import com.bwj.fintrack.transaction.dto.request.WithdrawRequest;
import com.bwj.fintrack.transaction.dto.response.WithdrawResponse;
import com.bwj.fintrack.transaction.entity.Transaction;
import com.bwj.fintrack.transaction.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 출금(Withdraw) 거래를 표현하는 커맨드
 *
 * 책임:
 *  - 금액/한도 검증
 *  - 계좌 조회 & 상태 검증
 *  - 잔액/최소유지금 정책 검증
 *  - 실제 잔액 감소 (account.withdraw)
 *  - 거래 로그(Transaction) 생성 및 저장
 *  - 최종 응답 DTO 반환
 *
 * 즉 "출금이라는 비즈니스 유스케이스 1건"을 완전히 캡슐화한 객체.
 */
public class WithdrawCommand implements TransactionCommand<WithdrawResponse> {

    private final WithdrawRequest request;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountValidator accountValidator;

    public WithdrawCommand(WithdrawRequest request,
                           AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           AccountValidator accountValidator) {

        this.request = request;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountValidator = accountValidator;
    }

    @Override
    public WithdrawResponse execute() {

        // 1) 금액 기본 정책 검증 (양수, 단일거래 한도 초과 여부 등)
        accountValidator.validatePositiveAmount(request.amount());
        accountValidator.validateMaxTxAmount(request.amount());

        // 2) 계좌 조회 (락 걸어서 동시성 제어)
        Account account = accountRepository.findWithLockByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 3) 소유자 존재 여부 / 상태(ACTIVE) 등 검증
        accountValidator.validateOwnerPresent(account);
        accountValidator.validateActive(account);

        // 4) 금액 스케일 정규화 (소수 둘째 자리)
        BigDecimal normalizedAmount = normalizeScale2(request.amount());

        // 5) 인출 가능 여부 (잔액 부족, 최소 유지 잔액 위반 등)
        accountValidator.validateWithdrawPossible(account, normalizedAmount);

        // 6) 실제 출금 수행 (도메인 로직)
        account.withdraw(normalizedAmount);

        // 7) 거래 로그 생성
        Transaction tx = Transaction.withdrawalSuccess(
                account,
                normalizedAmount,
                request.methodType(),
                request.memo()
        );

        // 8) 로그 저장
        Transaction savedTx = transactionRepository.save(tx);

        // 9) 응답 DTO로 변환
        return WithdrawResponse.from(savedTx);
    }

    private BigDecimal normalizeScale2(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public TransactionCommand<?> compensate() {
        // "출금 취소" 같은 보상 트랜잭션을 여기서 만들 수 있음.
        // 예: 동일 금액 입금 커맨드를 만들어 반환하는 식으로 확장 가능.
        throw new UnsupportedOperationException("Withdraw reversal is not yet implemented.");
    }
}
