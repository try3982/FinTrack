package com.bwj.fintrack.transaction.dto.response;

import java.util.List;

public record TransactionHistoryPageResponse(
        List<TransactionHistoryItemResponse> items,
        String nextCursor, // "2025-10-12T09:22:11.331|c7a1b..." 형태
        boolean hasNext
) {
    public static TransactionHistoryPageResponse of(
            List<TransactionHistoryItemResponse> items,
            String nextCursor,
            boolean hasNext
    ) {
        return new TransactionHistoryPageResponse(items, nextCursor, hasNext);
    }
}