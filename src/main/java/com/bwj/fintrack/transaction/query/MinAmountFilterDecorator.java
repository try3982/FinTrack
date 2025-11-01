package com.bwj.fintrack.transaction.query;


import com.bwj.fintrack.transaction.dto.response.TransactionHistoryItemResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * 금액이 minAmount 이상인 거래만 남긴다.
 * (예: "1만원 이상 거래만 보고 싶어요")
 */
public class MinAmountFilterDecorator extends TransactionHistoryDecorator {

    private final BigDecimal minAmount;

    public MinAmountFilterDecorator(
            TransactionHistoryViewProvider delegate,
            BigDecimal minAmount
    ) {
        super(delegate);
        this.minAmount = minAmount;
    }

    @Override
    public List<TransactionHistoryItemResponse> items() {
        return delegate.items().stream()
                .filter(item -> item.amount().compareTo(minAmount) >= 0)
                .toList();
    }
}
