package com.bwj.fintrack.autotransfer.command;

import com.bwj.fintrack.account.entity.Account;
import com.bwj.fintrack.account.repository.AccountRepository;
import com.bwj.fintrack.autotransfer.dto.request.CreateAutoTransferRequest;
import com.bwj.fintrack.autotransfer.entity.AutoTransfer;
import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 자동이체 예약 스케줄을 새로 등록하는 유스케이스를 캡슐화한 Command.
 *
 * 역할:
 *  - 출금/입금 계좌 유효성 검증 (계좌 존재, ACTIVE 여부, 소유자 확인 등)
 *  - 최초 nextRunAt 계산
 *  - AutoTransfer 엔티티 생성
 *
 * 주의:
 *  - 돈을 움직이지 않는다. (스케줄만 만든다.)
 *  - 저장(persist)은 Service 레이어에서 수행해도 되고,
 *    여기서 리포지토리 받아서 save까지 해도 된다. (프로젝트 스타일 선택)
 */
public class CreateAutoTransferCommand {

    private final CreateAutoTransferRequest request;
    private final AccountRepository accountRepository;

    public CreateAutoTransferCommand(CreateAutoTransferRequest request,
                                     AccountRepository accountRepository) {
        this.request = request;
        this.accountRepository = accountRepository;
    }

    public AutoTransfer buildNewSchedule() {

        // 1. 출금 계좌 조회 + 본인 소유인지 + ACTIVE인지
        Account from = accountRepository.findByAccountNumber(request.fromAccountNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (from.getUser() == null ||
                !from.getUser().getId().equals(request.userId())) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCOUNT_ACCESS);
        }
        if (!from.isActive()) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 2. 입금 계좌 조회 + ACTIVE인지
        Account to = accountRepository.findByAccountNumber(request.toAccountNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (!to.isActive()) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 3. 최초 nextRunAt 계산
        LocalDateTime firstNextRunAt = computeInitialNextRunAt(
                request.dayOfMonth(),
                request.runTime()
        );

        // 4. AutoTransfer 엔티티 인스턴스 생성 (아직 save 전)
        return AutoTransfer.builder()
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
    }

    // ===== 내부 유틸 =====

    /**
     * "지금 시각 이후 가장 가까운 실행 예정 시각"을 계산
     * - 오늘 month/dayOfMonth/runTime 조합이 미래면 그걸 사용
     * - 이미 지났으면 다음 달로 미룸
     * - 말일 보정 포함
     */
    private LocalDateTime computeInitialNextRunAt(Integer dayOfMonth, String runTime) {
        LocalTime time = LocalTime.parse(runTime); // "HH:mm"
        LocalDate today = LocalDate.now();

        LocalDate firstDate = adjustDayOfMonth(today.getYear(), today.getMonthValue(), dayOfMonth);
        LocalDateTime candidate = LocalDateTime.of(firstDate, time);

        if (candidate.isBefore(LocalDateTime.now())) {
            LocalDate nextMonthDate = adjustDayOfMonth(
                    today.plusMonths(1).getYear(),
                    today.plusMonths(1).getMonthValue(),
                    dayOfMonth
            );
            return LocalDateTime.of(nextMonthDate, time);
        }
        return candidate;
    }

    /**
     * 31일 예약인데 다음 달이 30일까지면 30일로,
     * 31일 예약인데 2월이면 28/29일로 보정.
     */
    private LocalDate adjustDayOfMonth(int year, int month, int desiredDay) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int lastDay = firstDay.lengthOfMonth();
        int safeDay = Math.min(desiredDay, lastDay);
        return LocalDate.of(year, month, safeDay);
    }
}
