package com.bwj.fintrack.autotransfer.service;



import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.autotransfer.entity.AutoTransfer;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import com.bwj.fintrack.transaction.entity.Transaction;
import com.bwj.fintrack.transaction.entity.TransactionMethodType;
import com.bwj.fintrack.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 단일 AutoTransfer를 실제로 이행하는 도메인 서비스.
 * 책임:
 *  - 잔액 이동(출금/입금)
 *  - 거래내역(Transaction) 생성
 *
 * 비즈니스 규칙(잔액 부족, 계좌 비활성 등)은 Account 도메인이 던지는 예외를 그대로 서비스가 번역해서 처리하도록 설계 가능.
 * 여기서는 실패 시 예외를 그대로 밖으로 던지게 해서 위에서 failCount 증가시키도록 한다.
 */
@Service
@RequiredArgsConstructor
public class AutoTransferExecutionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * 실행 대상 AutoTransfer 한 건에 대해
     * fromAccount -> toAccount 로 금액을 이체하고
     * 거래내역(Transaction)을 양쪽 모두에 기록한다.
     *
     * 성공하면 두 계좌 상태까지 commit 된다.
     */
    @Transactional
    public void executeAutoTransfer(AutoTransfer schedule) {
        Account from = schedule.getFromAccount();

        Account to = accountRepository.findByAccountNumber(schedule.getToAccountNo())
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 1) 계좌 상태/소유권 등의 유효성 검증
        // (여기선 간단히 "둘 다 active인지" 정도만 체크. 필요하면 추가 검증 가능)
        if (!from.isActive()) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        if (!to.isActive()) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        BigDecimal amount = schedule.getAmount();

        // 2) 출금
        from.withdraw(amount);
        // 출금 트랜잭션 로그
        Transaction outTx = Transaction.transferOutSuccess(
                from,
                amount,
                TransactionMethodType.AUTO,
                "[AUTO] 정기이체 -> " + schedule.getToAccountNo()
        );
        transactionRepository.save(outTx);

        // 3) 입금
        to.deposit(amount);
        // 입금 트랜잭션 로그
        Transaction inTx = Transaction.transferInSuccess(
                to,
                amount,
                TransactionMethodType.AUTO,
                "[AUTO] 정기이체 입금 from " + from.getAccountNumber()
        );
        transactionRepository.save(inTx);

        // 여기까지 예외 없이 왔다면 정상 이체 완료
    }
}
