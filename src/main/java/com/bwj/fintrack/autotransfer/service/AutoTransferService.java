package com.bwj.fintrack.autotransfer.service;

import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.autotransfer.dto.request.CreateAutoTransferRequest;
import com.bwj.fintrack.autotransfer.dto.response.CreateAutoTransferResponse;
import com.bwj.fintrack.autotransfer.entity.AutoTransfer;
import com.bwj.fintrack.autotransfer.repository.AutoTransferRepository;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class AutoTransferService {

    private final AccountRepository accountRepository;
    private final AutoTransferRepository autoTransferRepository;

    @Transactional
    public CreateAutoTransferResponse createAutoTransfer(CreateAutoTransferRequest request) {

        // 1) fromAccount 로드 + 소유자 검증
        Account from = accountRepository.findByAccountNumber(request.fromAccountNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (from.getUser() == null ||
                !from.getUser().getId().equals(request.userId())) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCOUNT_ACCESS);
        }
        if (!from.isActive()) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 2) toAccount 존재 및 ACTIVE 확인
        Account to = accountRepository.findByAccountNumber(request.toAccountNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (!to.isActive()) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 3) nextRunAt 계산
        LocalDateTime firstNextRunAt = computeInitialNextRunAt(
                request.dayOfMonth(),
                request.runTime()
        );

        // 4) 엔티티 생성
        AutoTransfer at = AutoTransfer.builder()
                .fromAccount(from)
                .toAccountNo(to.getAccountNumber())
                .amount(request.amount().setScale(2, RoundingMode.HALF_UP))
                .dayOfMonth(request.dayOfMonth())
                .runTime(request.runTime())
                .nextRunAt(firstNextRunAt)
                .lastRunAt(null)
                .active(true)
                .failCount(0)
                .maxRetries(3)
                .build();

        AutoTransfer saved = autoTransferRepository.save(at);

        return CreateAutoTransferResponse.from(saved);
    }

    /**
     * 최초 nextRunAt 계산 규칙:
     * - 오늘 기준으로 dayOfMonth/runTime 조합한 시각을 만든다.
     * - 그 시각이 지금보다 이전이면 다음 달로 민다.
     * - 말일(30,31일) 이슈는 AutoTransfer 엔티티에서 다음 달 갱신할 때 보정하지만,
     *   최초 생성 시에도 비슷하게 보정한다.
     */
    private LocalDateTime computeInitialNextRunAt(Integer dayOfMonth, String runTime) {
        LocalTime time = LocalTime.parse(runTime); // "HH:mm"
        LocalDate today = LocalDate.now();

        LocalDate firstDate = adjustDayOfMonth(today.getYear(), today.getMonthValue(), dayOfMonth);
        LocalDateTime candidate = LocalDateTime.of(firstDate, time);

        if (candidate.isBefore(LocalDateTime.now())) {
            // 이미 지나간 시간이라면 다음 달
            LocalDate nextMonthDate = adjustDayOfMonth(
                    today.plusMonths(1).getYear(),
                    today.plusMonths(1).getMonthValue(),
                    dayOfMonth
            );
            return LocalDateTime.of(nextMonthDate, time);
        }
        return candidate;
    }

    // 말일 보정: 해당 월에 dayOfMonth가 없으면 그 달의 마지막 날로 고정
    private LocalDate adjustDayOfMonth(int year, int month, int desiredDay) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int lastDay = firstDay.lengthOfMonth();
        int day = Math.min(desiredDay, lastDay);
        return LocalDate.of(year, month, day);
    }
}
