package com.bwj.fintrack.transaction.service;

import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.account.service.AccountValidator;
import com.bwj.fintrack.transaction.dto.request.TransactionHistoryRequest;
import com.bwj.fintrack.transaction.dto.response.TransactionHistoryItemResponse;
import com.bwj.fintrack.transaction.dto.response.TransactionHistoryPageResponse;
import com.bwj.fintrack.transaction.query.BaseTransactionHistoryViewProvider;
import com.bwj.fintrack.transaction.query.TransactionHistoryViewProvider;
import com.bwj.fintrack.transaction.repository.TransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionHistoryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final AccountRepository accountRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final AccountValidator accountValidator;

    @Transactional(readOnly = true)
    public TransactionHistoryPageResponse getTransactionHistory(TransactionHistoryRequest request) {

        // 1) 기본 Provider 생성
        TransactionHistoryViewProvider provider =
                new BaseTransactionHistoryViewProvider(
                        request,
                        accountRepository,
                        transactionHistoryRepository
                );

        List<TransactionHistoryItemResponse> items = provider.items();
        String nextCursor = provider.nextCursor();
        boolean hasNext = provider.hasNext();

        return TransactionHistoryPageResponse.from(items, nextCursor, hasNext);
    }

    private int normalizePageSize(Integer size) {
        int s = (size == null) ? DEFAULT_PAGE_SIZE : size;
        if (s < 1) s = 1;
        if (s > MAX_PAGE_SIZE) s = MAX_PAGE_SIZE;
        return s;
    }

    private UUID tryParseUuid(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String buildCursor(LocalDateTime dt, UUID id) {
        return dt.toString() + "|" + id;
    }
}
