package com.bwj.fintrack.transaction.query;


import com.bwj.fintrack.transaction.dto.response.TransactionHistoryItemResponse;

import java.util.Comparator;
import java.util.List;

/**
 * 거래내역을 오래된 순(오름차순)으로 정렬해 반환하는 데코레이터
 * nextCursor / hasNext 는 그대로 유지(즉 페이징은 여전히 desc기준),
 * 단지 화면 표시 순서만 바꾼다.
 */
public class OldestFirstSortDecorator extends TransactionHistoryDecorator {

    public OldestFirstSortDecorator(TransactionHistoryViewProvider delegate) {
        super(delegate);
    }

    @Override
    public List<TransactionHistoryItemResponse> items() {
        return delegate.items().stream()
                .sorted(Comparator.comparing(TransactionHistoryItemResponse::transactedAt))
                .toList();
    }
}
