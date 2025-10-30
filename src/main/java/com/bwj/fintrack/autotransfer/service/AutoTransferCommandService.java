package com.bwj.fintrack.autotransfer.service;


import com.bwj.fintrack.autotransfer.dto.request.CancelAutoTransferRequest;
import com.bwj.fintrack.autotransfer.dto.request.UpdateAutoTransferBody;
import com.bwj.fintrack.autotransfer.dto.response.AutoTransferItemResponse;
import com.bwj.fintrack.autotransfer.entity.AutoTransfer;
import com.bwj.fintrack.autotransfer.repository.AutoTransferRepository;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.math.BigDecimal;
import java.time.*;

@Service
@RequiredArgsConstructor
public class AutoTransferCommandService {

    private final AutoTransferRepository autoTransferRepository;

    @Transactional
    public AutoTransferItemResponse updateAutoTransfer(Long autoTransferId,
                                                       UpdateAutoTransferBody body) {

        // 1) 요청자 식별
        if (body.userId() == null) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCOUNT_ACCESS);
        }

        // 2) 대상 자동이체 조회
        AutoTransfer at = autoTransferRepository.findById(autoTransferId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTO_TRANSFER_NOT_FOUND));

        // 3) 권한 확인: 이 자동이체의 fromAccount 소유자가 내가 맞는지
        if (at.getFromAccount().getUser() == null ||
                !at.getFromAccount().getUser().getId().equals(body.userId())) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCOUNT_ACCESS);
        }

        // 4) 값 정규화
        BigDecimal normalizedAmount = body.amount().setScale(2, RoundingMode.HALF_UP);
        LocalTime runTime = parseRunTime(body.runTime());
        LocalDateTime nextRunAt = computeNextRunAt(body.dayOfMonth(), runTime);

        // 5) 엔티티에 변경 적용
        at.updateSchedule(
                body.toAccountNumber(),
                normalizedAmount,
                body.dayOfMonth(),
                body.runTime(),
                nextRunAt,
                body.active()
        );

        AutoTransfer saved = autoTransferRepository.save(at);
        return AutoTransferItemResponse.from(saved);
    }

    @Transactional
    public AutoTransferItemResponse cancelAutoTransfer(Long autoTransferId,
                                                       CancelAutoTransferRequest request) {

        // 1) 누가 요청했는지 확인 (인증 미도입 상태라 직접 받음)
        if (request.userId() == null) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCOUNT_ACCESS);
        }

        // 2) 자동이체 엔티티 조회
        AutoTransfer at = autoTransferRepository.findById(autoTransferId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTO_TRANSFER_NOT_FOUND));

        // 3) 소유자 검증
        if (at.getFromAccount().getUser() == null ||
                !at.getFromAccount().getUser().getId().equals(request.userId())) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCOUNT_ACCESS);
        }

        // 4) 비활성화 (멱등: 이미 비활성화 상태여도 그냥 false 유지)
        at.deactivate();

        // 5) 저장 후 반환
        AutoTransfer saved = autoTransferRepository.save(at);
        return AutoTransferItemResponse.from(saved);
    }

    private LocalTime parseRunTime(String runTime) {
        String[] parts = runTime.split(":");
        int hour = Integer.parseInt(parts[0]);
        int min  = Integer.parseInt(parts[1]);
        return LocalTime.of(hour, min);
    }

    private LocalDateTime computeNextRunAt(Integer dayOfMonth, LocalTime runTime) {
        LocalDate today = LocalDate.now();

        int year = today.getYear();
        int month = today.getMonthValue();

        LocalDate candidate = LocalDate.of(
                year,
                month,
                Math.min(dayOfMonth, lastDayOfMonth(year, month))
        );
        LocalDateTime candidateDateTime = LocalDateTime.of(candidate, runTime);

        if (candidateDateTime.isBefore(LocalDateTime.now())) {
            // 이미 지난 시간이면 다음 달로 밀기
            int nextYear = year;
            int nextMonth = month + 1;
            if (nextMonth == 13) {
                nextMonth = 1;
                nextYear++;
            }
            LocalDate nextCandidate = LocalDate.of(
                    nextYear,
                    nextMonth,
                    Math.min(dayOfMonth, lastDayOfMonth(nextYear, nextMonth))
            );
            return LocalDateTime.of(nextCandidate, runTime);
        }

        return candidateDateTime;
    }

    private int lastDayOfMonth(int year, int month) {
        return YearMonth.of(year, month).lengthOfMonth();
    }
}
