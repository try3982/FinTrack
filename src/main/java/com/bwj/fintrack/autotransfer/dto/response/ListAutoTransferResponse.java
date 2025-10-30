package com.bwj.fintrack.autotransfer.dto.response;

import java.util.List;

/**
 * 자동이체 규칙 목록 응답 래퍼
 * - 확장성을 위해 리스트를 한 번 감싸준다.
 *   (추후 paging, totalCount 등의 메타데이터를 추가하기 쉽다)
 */
public record ListAutoTransferResponse(
        List<AutoTransferItemResponse> items
) {
    public static ListAutoTransferResponse from(List<AutoTransferItemResponse> list) {
        return new ListAutoTransferResponse(list);
    }
}