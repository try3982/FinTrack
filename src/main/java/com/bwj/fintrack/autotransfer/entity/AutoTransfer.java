package com.bwj.fintrack.autotransfer.entity;

import com.bwj.fintrack.account.entity.Account;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 매월 정해진 날짜(dayOfMonth)와 시각(runTime, "HH:mm")에
 * fromAccount -> toAccountNo 로 amount 만큼 자동이체를 시도하는 예약 규칙.
 *
 * nextRunAt 은 "다음에 실행할 정확한 시각"
 * lastRunAt 은 "마지막으로 시도한 시각"
 *
 * failCount/maxRetries 는 반복 실패 제어용.
 */
@Entity
@Table(name = "f_auto_transfers",
        indexes = @Index(name = "ix_auto_active_next", columnList = "active, next_run_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
public class AutoTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 출금 주체 계좌 (우리가 소유권 검증할 대상)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    // 받는 쪽 계좌번호 (상대방 소유일 수도 있음)
    @Column(name = "to_account_no", nullable = false, length = 16)
    private String toAccountNo;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    // 매달 몇 일에 실행할지 (1~31)
    @Column(name = "day_of_month", nullable = false)
    private Integer dayOfMonth;

    // 실행 시간 "HH:mm" (예: "09:30")
    @Column(name = "run_time", nullable = false, length = 5)
    private String runTime;

    // 다음 실행 예정 시각 (스케줄러는 이 값을 보고 실행 여부 판단)
    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime nextRunAt;

    // 마지막 실행 시각
    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    // 자동이체 정책이 활성 상태인지 여부
    @Column(nullable = false)
    private boolean active;

    // 연속 실패 횟수
    @Column(name = "fail_count", nullable = false)
    private int failCount;

    // 연속 실패 허용 한도 (초과하면 active=false 로 비활성화 해도 됨)
    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 3;

    @Version
    private Long version;

    /**
     * 실행 조건:
     * - active == true
     * - nextRunAt <= now
     * - failCount <= maxRetries
     */
    public boolean isDue(LocalDateTime now) {
        if (!active) return false;
        if (failCount > maxRetries) return false;
        return !nextRunAt.isAfter(now);
    }

    /**
     * 실행 시도 직전에 호출:
     * - 마지막 시도 시간 갱신
     */
    public void markAttempt(LocalDateTime now) {
        this.lastRunAt = now;
    }

    /**
     * 성공적으로 이체가 끝났을 때:
     * - failCount 초기화
     * - nextRunAt 을 "다음 달 동일 규칙"으로 갱신
     */
    public void markSuccessAndScheduleNext() {
        this.failCount = 0;
        this.nextRunAt = computeNextRunAtAfter(this.nextRunAt);
    }

    /**
     * 실패 시:
     * - failCount 증가
     * - (선택) 일정 정책: 그래도 nextRunAt 을 다음 달로 미룰지, 아니면 그대로 둘지?
     *   여기선 "그대로 둠"으로 해서 재시도 기회를 주되,
     *   failCount가 max를 넘으면 active=false로 비활성화 가능.
     */
    public void markFailure() {
        this.failCount = this.failCount + 1;
        if (this.failCount > this.maxRetries) {
            this.active = false;
        }
    }

    /**
     * nextRunAt 을 기준으로 다음 달 같은 day_of_month/run_time 시각을 계산.
     * 말일 보정(30/31일 문제)은 간단하게 '해당 월에 dayOfMonth가 없으면 그 달의 말일'로 맞춘다.
     */
    private LocalDateTime computeNextRunAtAfter(LocalDateTime base) {
        LocalDate baseDate = base.toLocalDate().plusMonths(1);
        LocalDate adjustedDate = adjustDayOfMonth(baseDate.getYear(), baseDate.getMonthValue(), this.dayOfMonth);
        LocalTime time = LocalTime.parse(this.runTime); // "HH:mm"
        return LocalDateTime.of(adjustedDate, time);
    }

    /**
     * 주어진 연/월에서 dayOfMonth가 존재하지 않으면 그 달의 마지막 날로 보정
     * (예: 31일 지정인데 2월이면 2월 말일로 스케줄)
     */
    private LocalDate adjustDayOfMonth(int year, int month, int desiredDay) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int lastDay = firstDay.lengthOfMonth();
        int day = Math.min(desiredDay, lastDay);
        return LocalDate.of(year, month, day);
    }

    /**
     * 자동이체 예약 정보를 수정한다.
     * nextRunAt은 서비스에서 계산해준 값을 그대로 받는다.
     * 정책적으로 failCount는 초기화(0)한다.
     */
    public void updateSchedule(String newToAccountNo,
                               BigDecimal newAmount,
                               Integer newDayOfMonth,
                               String newRunTime,
                               LocalDateTime newNextRunAt,
                               boolean newActive) {

        this.toAccountNo = newToAccountNo;
        this.amount = newAmount;
        this.dayOfMonth = newDayOfMonth;
        this.runTime = newRunTime;
        this.nextRunAt = newNextRunAt;
        this.active = newActive;

        // 사용자가 설정을 바꿨다는 건 새로운 조건으로 다시 시도할 거라는 의미이므로
        this.failCount = 0;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isActive() {
        return this.active;
    }
}
