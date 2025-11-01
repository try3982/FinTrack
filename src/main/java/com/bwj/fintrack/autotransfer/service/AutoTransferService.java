package com.bwj.fintrack.autotransfer.service;

import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.autotransfer.command.CreateAutoTransferCommand;
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

        // 커맨드에 유스케이스 정책(검증, nextRunAt 계산 등)을 위임
        CreateAutoTransferCommand command =
                new CreateAutoTransferCommand(request, accountRepository);

        AutoTransfer schedule = command.buildNewSchedule();

        AutoTransfer saved = autoTransferRepository.save(schedule);

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
