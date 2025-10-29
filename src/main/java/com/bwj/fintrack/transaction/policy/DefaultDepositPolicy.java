//package com.bwj.fintrack.transaction.policy;
//
//
//import com.bwj.fintrack.account.entity.Account;
//import com.bwj.fintrack.common.exception.custom.CustomException;
//import com.bwj.fintrack.common.exception.response.ErrorCode;
//
//import java.math.BigDecimal;
//
//public class DefaultDepositPolicy implements DepositPolicy {
//
//    private final BigDecimal maxTxAmount;
//    private final BigDecimal maxBalance;
//
//    public DefaultDepositPolicy(BigDecimal maxTxAmount, BigDecimal maxBalance) {
//        this.maxTxAmount = maxTxAmount;
//        this.maxBalance = maxBalance;
//    }
//
//    @Override
//    public void validate(Account account, BigDecimal amount) {
//        if (amount == null || amount.signum() <= 0) {
//            throw new CustomException(ErrorCode.AMOUNT_MUST_BE_POSITIVE);
//        }
//        if (maxTxAmount != null && amount.compareTo(maxTxAmount) > 0) {
//            throw new CustomException(ErrorCode.INVALID_AMOUNT);
//        }
//        BigDecimal after = account.previewAfterDeposit(amount);
//        if (maxBalance != null && after.compareTo(maxBalance) > 0) {
//            throw new CustomException(ErrorCode.INVALID_AMOUNT);
//        }
//    }
//}
