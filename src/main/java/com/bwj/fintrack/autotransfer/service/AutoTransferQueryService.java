package com.bwj.fintrack.autotransfer.service;

import com.bwj.fintrack.autotransfer.dto.request.ListAutoTransferRequest;
import com.bwj.fintrack.autotransfer.dto.response.AutoTransferItemResponse;
import com.bwj.fintrack.autotransfer.dto.response.ListAutoTransferResponse;
import com.bwj.fintrack.autotransfer.entity.AutoTransfer;
import com.bwj.fintrack.autotransfer.repository.AutoTransferRepository;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoTransferQueryService {

    private final AutoTransferRepository autoTransferRepository;

    @Transactional(readOnly = true)
    public ListAutoTransferResponse listUserAutoTransfers(ListAutoTransferRequest request) {

        // 1) userId 없으면 접근 불가
        if (request.userId() == null) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCOUNT_ACCESS);
        }

        // 2) 해당 사용자의 자동이체 규칙 전부 조회
        List<AutoTransfer> rules =
                autoTransferRepository.findByFromAccount_User_IdOrderByNextRunAtAsc(request.userId());

        // 3) DTO 매핑
        List<AutoTransferItemResponse> items = rules.stream()
                .map(AutoTransferItemResponse::from)
                .toList();

        return ListAutoTransferResponse.from(items);
    }
}