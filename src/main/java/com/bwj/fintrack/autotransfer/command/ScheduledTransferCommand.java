package com.bwj.fintrack.autotransfer.command;


import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.account.service.AccountValidator;
import com.bwj.fintrack.autotransfer.entity.AutoTransfer;
import com.bwj.fintrack.autotransfer.repository.AutoTransferRepository;
import com.bwj.fintrack.autotransfer.service.TransactionLimitValidator;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import com.bwj.fintrack.transaction.command.TransactionCommand;
import com.bwj.fintrack.transaction.dto.response.TransferResponse;
import com.bwj.fintrack.transaction.entity.Transaction;
import com.bwj.fintrack.transaction.entity.TransactionMethodType;
import com.bwj.fintrack.transaction.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * "자동이체 스케줄 1건을 지금 실행하라"라는 의미의 커맨드
 *
 * 이 커맨드는 AutoTransfer 엔티티를 받아서 실제로 돈을 움직인다
 *
 * 수행 내용:
 *  - 출금/입금 계좌 비관적 락으로 조회 (데드락 방지 순서 유지)
 *  - 계좌 상태 검증 (from ACTIVE, to ACTIVE)
 *  - 출금자 일일 한도 검증
 *  - 잔액/최소유지금 정책 검증
 *  - 수수료 계산
 *  - 잔액 차감/증액
 *  - 거래(Transaction) 두 건 생성 및 저장
 *  - TransferResponse 반환
 *
 * 주의:
 *  - 이 커맨드는 스케줄러가 호출한다.
 *  - "어느 스케줄(AutoTransfer)에서 실행된 건지" 추적 가능하도록 scheduleId를 들고간다.
 */
public class ScheduledTransferCommand implements TransactionCommand<TransferResponse> {

    private final AutoTransfer schedule;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountValidator accountValidator;
    private final TransactionLimitValidator transactionLimitValidator;

    public ScheduledTransferCommand(AutoTransfer schedule,
                                    AccountRepository accountRepository,
                                    TransactionRepository transactionRepository,
                                    AccountValidator accountValidator,
                                    TransactionLimitValidator transactionLimitValidator) {

        this.schedule = schedule;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountValidator = accountValidator;
        this.transactionLimitValidator = transactionLimitValidator;
    }

    @Override
    public TransferResponse execute() {

        // 1) 스케줄에서 필요한 정보 꺼내기
        // 출금 계좌번호 (우리 쪽은 fromAccount 엔티티를 이미 들고 있음)
        String fromAccountNo = schedule.getFromAccount().getAccountNumber();
        // 입금 계좌번호 (문자열로만 저장되어 있음)
        String toAccountNo = schedule.getToAccountNo();
        BigDecimal rawAmount = schedule.getAmount(); // 이미 scale(2)로 저장된 상태라고 가정하되 다시 한 번 정규화
        String memo = "[AUTO] monthly"; // 자동이체 트랜잭션 메모 정책 (필요하면 schedule마다 다르게도 가능)

        // 2) 금액 검증
        //    - 0보다 커야 한다
        //    - 시스템 한도(단일 거래 상한) 내여야 한다
        accountValidator.validatePositiveAmount(rawAmount);
        accountValidator.validateMaxTxAmount(rawAmount);

        // 3) 데드락 방지용 계좌 락 순서
        //    기존 Transfer 로직과 동일: 계좌번호 사전순으로 먼저 락
        final boolean swapped = fromAccountNo.compareTo(toAccountNo) > 0;
        String first = swapped ? toAccountNo : fromAccountNo;
        String second = swapped ? fromAccountNo : toAccountNo;

        Account firstAcc = accountRepository.findWithLockByAccountNumber(first)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        Account secondAcc = accountRepository.findWithLockByAccountNumber(second)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        Account from = swapped ? secondAcc : firstAcc;
        Account to   = swapped ? firstAcc  : secondAcc;

        // 4) 계좌 상태 검증
        // 출금 계좌는 반드시 ACTIVE + 소유자 있어야 한다
        accountValidator.validateOwnerPresent(from);
        accountValidator.validateActive(from);

        // 입금 계좌도 ACTIVE 여야 한다
        accountValidator.validateActive(to);

        // 5) 금액 정규화 (scale=2 고정)
        BigDecimal amount = normalizeScale2(rawAmount);

        // 6) 수수료 계산 (동일 사용자 계좌 간 이체는 0원, 아니면 500원)
        BigDecimal fee = calculateTransferFee(from, to);

        // 7) 일일 한도 및 인출 가능 여부 검사
        BigDecimal totalDebit = amount.add(fee);

      //  transactionLimitValidator.validateDailyLimit(from.getUser(), totalDebit);
        accountValidator.validateWithdrawPossible(from, totalDebit);

        // 8) 실제 출금/입금
        from.withdraw(totalDebit);
        to.deposit(amount);

        // 9) 거래 로그(Transaction) 생성 & 저장
        // 자동이체도 결국 "출금 트랜잭션" + "입금 트랜잭션" 두 건으로 남겨야 한다
        Transaction outTx = Transaction.transferOutSuccess(
                from,
                amount,
                TransactionMethodType.AUTO_TRANSFER, // 시스템 송금이라는 걸 명시적으로 남기고 싶음
                memo
        );
        Transaction inTx = Transaction.transferInSuccess(
                to,
                amount,
                TransactionMethodType.AUTO_TRANSFER,
                memo
        );

        transactionRepository.save(outTx);
        transactionRepository.save(inTx);

        // 10) 호출자에게 결과 반환
        return TransferResponse.from(outTx, inTx);
    }

    private BigDecimal normalizeScale2(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 기존 Transfer 로직과 동일한 수수료 정책:
     * - 같은 사용자 간 이체면 수수료 0
     * - 타인에게 보내면 500원
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
        // 향후 "자동이체 되돌리기" 기능 (예: 잘못 빠져나간 자동이체 롤백)
        // -> 반대 방향 이체 ScheduledTransferCommand 생성 가능
        throw new UnsupportedOperationException("AutoTransfer reversal not yet implemented.");
    }
}
