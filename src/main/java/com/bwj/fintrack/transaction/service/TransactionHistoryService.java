package com.bwj.fintrack.transaction.service;

import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import com.bwj.fintrack.transaction.dto.request.TransactionHistoryRequest;
import com.bwj.fintrack.transaction.dto.response.TransactionHistoryItemResponse;
import com.bwj.fintrack.transaction.dto.response.TransactionHistoryPageResponse;
import com.bwj.fintrack.transaction.entity.Transaction;
import com.bwj.fintrack.transaction.repository.TransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionHistoryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final AccountRepository accountRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    @Transactional(readOnly = true)
    public TransactionHistoryPageResponse getTransactionHistory(TransactionHistoryRequest request) {

        // 1) 요청자 식별 / 권한 검증용 값 존재 확인
        if (request.userId() == null) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCOUNT_ACCESS);
        }

        // 2) 계좌 로드
        Account account = accountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 3) 본인 계좌인지 확인
        if (account.getUser() == null || !account.getUser().getId().equals(request.userId())) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCOUNT_ACCESS);
        }

        // 4) 기간 계산
        LocalDateTime from = null;
        LocalDateTime to = null;
        if (Boolean.TRUE.equals(request.onlyRecent3Months())) {
            LocalDateTime now = LocalDateTime.now();
            to = now;
            from = now.minus(3, ChronoUnit.MONTHS);
        }

        // 5) 페이지 사이즈 정규화
        int size = normalizePageSize(request.size());

        // 6) 커서 정보 파싱
        LocalDateTime cursorDate = request.cursorDate();
        UUID cursorId = tryParseUuid(request.cursorId());

        // 7) 조회: size+1 로 next 여부 감지
        List<Transaction> slice = transactionHistoryRepository.findPageForAccount(
                account.getId(),
                request.types(),
                from,
                to,
                size + 1,
                cursorDate,
                cursorId
        );

        boolean hasNext = slice.size() > size;
        if (hasNext) {
            slice = slice.subList(0, size); // 현재 페이지에 해당하는 부분만 남긴다
        }

        // 8) nextCursor 생성
        String nextCursor = null;
        if (hasNext && !slice.isEmpty()) {
            Transaction last = slice.get(slice.size() - 1);
            nextCursor = buildCursor(last.getTransactionDate(), last.getId());
        }

        // 9) DTO 변환
        List<TransactionHistoryItemResponse> items = slice.stream()
                .map(TransactionHistoryItemResponse::from)
                .toList();

        return TransactionHistoryPageResponse.of(items, nextCursor, hasNext);
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
