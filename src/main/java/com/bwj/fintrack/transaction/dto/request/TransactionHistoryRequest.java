package com.bwj.fintrack.transaction.dto.request;

import com.bwj.fintrack.transaction.entity.TransactionType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Set;

public record TransactionHistoryRequest(

        @NotNull
        Long userId, // 인증 미도입 상태라 body에서 받는다. 추후 SecurityContext로 대체 예정.

        @NotNull
        String accountNumber, // "123-4567-0000001" 형태. 필요하면 @Pattern 추가해도 된다.

        // 조회할 거래 타입 필터 (DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT 등)
        // null 또는 빈 Set이면 전체 조회
        Set<TransactionType> types,

        // 최근 3개월 내 거래만 볼지 여부. true면 강제로 now-3개월 ~ now 로 제한
        Boolean onlyRecent3Months,

        // 커서 기반 페이징용으로, "다음 페이지" 요청 시 프런트가 전달해준다
        // 첫 페이지면 둘 다 null
        LocalDateTime cursorDate,
        String cursorId,

        // 한 페이지 크기. null이면 기본값 20
        Integer size
) { }