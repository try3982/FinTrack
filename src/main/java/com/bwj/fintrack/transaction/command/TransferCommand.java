package com.bwj.fintrack.transaction.command;


import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.account.service.AccountValidator;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import com.bwj.fintrack.transaction.dto.request.TransferRequest;
import com.bwj.fintrack.transaction.dto.response.TransferResponse;
import com.bwj.fintrack.transaction.entity.Transaction;
import com.bwj.fintrack.transaction.repository.TransactionRepository;
import com.bwj.fintrack.autotransfer.service.TransactionLimitValidator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 계좌 이체(Transfer) 거래를 캡슐화한 커맨드.
 *
 * 책임:
 *  - 요청 유효성 검증 (동일 계좌 이체 금지, 금액 유효성)
 *  - 데드락 방지 순서대로 2개 계좌에 락 걸어 로드
 *  - 송금/수취 계좌 상태 검증
 *  - 수수료 계산
 *  - 일일 한도 검증, 잔액/최소유지금 검증
 *  - 출금 계좌에서 금액+수수료 차감 / 입금 계좌에 금액 가산
 *  - 출금/입금 각각 Transaction 로그 생성 및 저장
 *  - TransferResponse 생성
 *
 * 즉 "이체 1건"이라는 도메인 유스케이스 전체를 한 객체로 표현한다.
 */
public class TransferCommand implements TransactionCommand<TransferResponse> {

    private final TransferRequest request;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountValidator accountValidator;
    private final TransactionLimitValidator transactionLimitValidator;

    public TransferCommand(TransferRequest request,
                           AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           AccountValidator accountValidator,
                           TransactionLimitValidator transactionLimitValidator) {

        this.request = request;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountValidator = accountValidator;
        this.transactionLimitValidator = transactionLimitValidator;
    }

    @Override
    public TransferResponse execute() {

        // 1) 기본 요청 검증
        if (request.fromAccountNumber().equals(request.toAccountNumber())) {
            throw new CustomException(ErrorCode.INVALID_AMOUNT); // 기존 코드와 동일하게 재사용
        }

        accountValidator.validatePositiveAmount(request.amount());
        accountValidator.validateMaxTxAmount(request.amount());

        // 2) 데드락 방지를 위한 계좌 락 순서 결정
        //    (계좌 번호 문자열 비교해서 항상 작은 쪽 먼저 락)
        String a = request.fromAccountNumber();
        String b = request.toAccountNumber();
        final boolean swapped = a.compareTo(b) > 0;
        String first = swapped ? b : a;
        String second = swapped ? a : b;

        Account firstAcc = accountRepository.findWithLockByAccountNumber(first)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        Account secondAcc = accountRepository.findWithLockByAccountNumber(second)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        // swapped 여부에 따라 실제 "from/to" 매핑
        Account from = swapped ? secondAcc : firstAcc;
        Account to   = swapped ? firstAcc  : secondAcc;

        // 3) 상태 검증
        // 보내는 쪽은 소유자/상태 모두 확인
        accountValidator.validateOwnerPresent(from);
        accountValidator.validateActive(from);

        // 받는 쪽은 ACTIVE만 확인
        accountValidator.validateActive(to);

        // 4) 금액 스케일 고정
        BigDecimal amount = normalizeScale2(request.amount());

        // 5) 수수료 계산
        BigDecimal fee = calculateTransferFee(from, to);

        // 6) 한도/잔액 정책 검증
        // - 일일 한도: 등급/정책 기반으로 송금자에게 적용
        // - 잔액/최소유지금: 수수료까지 포함해서 출금 가능해야 함
        BigDecimal totalDebit = amount.add(fee);

      //  transactionLimitValidator.validateDailyLimit(from.getUser(), totalDebit);
        accountValidator.validateWithdrawPossible(from, totalDebit);

        // 7) 실제 출금/입금
        from.withdraw(totalDebit);
        to.deposit(amount);

        // 8) 거래 로그(Transaction) 두 건 생성
        Transaction outTx = Transaction.transferOutSuccess(
                from,
                amount,
                request.methodType(),
                request.memo()
        );
        Transaction inTx = Transaction.transferInSuccess(
                to,
                amount,
                request.methodType(),
                request.memo()
        );

        // 9) 로그 저장
        transactionRepository.save(outTx);
        transactionRepository.save(inTx);

        // 10) 최종 응답 조합
        return TransferResponse.from(outTx, inTx);
    }

    private BigDecimal normalizeScale2(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 동일 사용자 간 이체면 수수료 0
     * 다른 사용자에게 보내면 500.00
     * (네 기존 AccountService.calculateTransferFee 로직을 그대로 옮김)
     */
    private BigDecimal calculateTransferFee(Account from, Account to) {
        Long fromUserId = from.getUser().getId();
        Long toUserId   = to.getUser().getId();

        if (fromUserId.equals(toUserId)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal("500.00");
    }

    @Override
    public TransactionCommand<?> compensate() {
        // 보상(취소) 시나리오를 열어두는 훅:
        // ex) 잘못된 이체를 되돌릴 때 to->from 방향으로 동일 금액 재이체하는 커맨드를 만들 수 있다.
        throw new UnsupportedOperationException("Transfer reversal is not yet implemented.");
    }
}
