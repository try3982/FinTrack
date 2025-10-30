package com.bwj.fintrack.autotransfer.entity;

import com.bwj.fintrack.account.entity.Account;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 매월 특정 dayOfMonth / runTime("HH:mm")에
 * fromAccount -> toAccountNo 로 amount만큼 자동이체를 수행하는 예약 규칙.
 *
 * nextRunAt   : 다음 실행 예정 시각
 * lastRunAt   : 마지막 실행(또는 시도) 시각
 * active      : 예약 활성 여부
 * failCount   : 연속 실패 횟수
 * maxRetries  : 허용되는 최대 연속 실패 횟수
 *
 * 핵심 원칙:
 * - 상태 전이는 이 엔티티 내부 메서드로만 수행한다.
 *   (서비스 레이어는 "성공 처리해", "실패 처리해" 같은 의미적 요청만 한다.)
 */
@Entity
@Table(
        name = "f_auto_transfers",
        indexes = @Index(name = "ix_auto_active_next", columnList = "active, next_run_at")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
public class AutoTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 출금 계좌 (소유자 검증 대상)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    // 입금 계좌번호 (상대 계좌일 수도 있음)
    @Column(name = "to_account_no", nullable = false, length = 16)
    private String toAccountNo;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    // 매달 며칠(1~31)에 실행할지
    @Column(name = "day_of_month", nullable = false)
    private Integer dayOfMonth;

    // 실행 시간 "HH:mm" (예: "09:30")
    @Column(name = "run_time", nullable = false, length = 5)
    private String runTime;

    // 다음 실행 예정 시각 (스케줄러는 이걸 보고 실행 여부 판단)
    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime nextRunAt;

    // 마지막 실행(또는 시도) 시각
    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    // 이 자동이체 규칙이 현재 유효/활성인지 여부
    @Column(nullable = false)
    private boolean active;

    // 연속 실패 횟수
    @Column(name = "fail_count", nullable = false)
    private int failCount;

    // 허용 가능한 연속 실패 횟수. 초과하면 active=false로 꺼버릴 수 있음
    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 3;

    @Version
    private Long version;

    /* ==========================
     * 조회(상태판단) 계층
     * ========================== */

    /**
     * 지금 시점(now)에 이 규칙을 실행해도 되는지 여부.
     * 조건:
     *  - active == true
     *  - failCount <= maxRetries
     *  - nextRunAt <= now
     */
    public boolean isDue(LocalDateTime now) {
        if (!active) return false;
        if (failCount > maxRetries) return false;
        return !nextRunAt.isAfter(now);
    }

    /**
     * 규칙이 여전히 활성 상태인지 읽는 용도 (가독용 sugar)
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * 현재 연속 실패 횟수 반환
     * (서비스에서 로깅이나 모니터링용으로 쓸 수 있음)
     */
    public int currentFailCount() {
        return this.failCount;
    }

    /**
     * 허용되는 최대 실패 횟수
     */
    public int allowedMaxRetries() {
        return this.maxRetries;
    }


    /* ==========================
     * 상태 전이(명령) 계층
     * ========================== */

    /**
     * "이번 실행을 시도한다"라는 의미.
     * 마지막 시도 시각(lastRunAt)을 now로 갱신한다.
     * 실제 출금/입금은 서비스에서 수행하지만,
     * "우리는 이 규칙을 시도했다고 기록"하는 책임은 규칙 자신이 가진다.
     */
    public void markAttempt(LocalDateTime now) {
        this.lastRunAt = now;
    }

    /**
     * 자동이체가 정상적으로 수행되었을 때 호출.
     * - failCount 초기화
     * - nextRunAt 을 다음 달의 동일 규칙 시간으로 재계산
     * - lastRunAt 은 지금 시각으로 갱신
     */
    public void markSuccess(LocalDateTime now) {
        this.failCount = 0;
        this.lastRunAt = now;
        this.nextRunAt = computeNextRunAtAfter(this.nextRunAt);
        // active 상태는 유지 (true)
    }

    /**
     * 자동이체 수행에 실패했을 때 호출.
     * - failCount 증가
     * - failCount가 maxRetries 초과하면 active=false로 비활성화
     *   (초과 "이상"과 "초과" 중 어떤 정책을 쓸지 선택할 수 있는데,
     *    기존 코드에 맞춰 '>' 기준 유지)
     */
    public void markFailure() {
        this.failCount = this.failCount + 1;
        if (this.failCount > this.maxRetries) {
            this.active = false;
        }
        // nextRunAt은 그대로 두면 재시도 가능.
        // 정책적으로 "실패해도 일정은 내일로 미룬다" 같은 걸 하고 싶으면
        // 여기서 nextRunAt을 조정하면 된다.
    }

    /**
     * 사용자가 스케줄(금액/시각/활성 여부 등)을 직접 수정했을 때 호출.
     * 서비스 계층은 이미 모든 유효성 검증(금액 양수인지, 계좌 본인 것인지 등)을 끝낸 뒤
     * "이 값들로 바꿔"라고만 요청한다.
     *
     * 정책: 수정하면 failCount는 리셋(0). 새 조건으로 새롭게 시작하는 느낌.
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
        this.failCount = 0;
    }

    /**
     * 사용자가 "이 자동이체 끄고 싶어요" 했을 때 호출.
     * 그냥 active=false로 만든다.
     * (정책적으로 failCount는 그대로 둔다. 이건 '사용자 요청에 의한 비활성화'니까)
     */
    public void deactivate() {
        this.active = false;
    }


    /* ==========================
     * 내부 유틸 (스케줄 계산)
     * ========================== */

    /**
     * 주어진 base(보통 현재 nextRunAt) 기준으로
     * "다음 달 같은 dayOfMonth/runTime"을 계산한다.
     *
     * 말일 보정:
     *   31일 스케줄인데 다음 달이 30일까지면 30일로 조정,
     *   31일 스케줄인데 다음 달이 28일까지면 28일로 조정, 등.
     */
    private LocalDateTime computeNextRunAtAfter(LocalDateTime base) {
        LocalDate baseDatePlus1Month = base.toLocalDate().plusMonths(1);

        LocalDate adjustedDate = adjustDayOfMonth(
                baseDatePlus1Month.getYear(),
                baseDatePlus1Month.getMonthValue(),
                this.dayOfMonth
        );

        LocalTime time = LocalTime.parse(this.runTime); // "HH:mm"
        return LocalDateTime.of(adjustedDate, time);
    }

    /**
     * 해당 (year, month)에서 원하는 dayOfMonth가 없다면
     * 그 달의 말일로 보정한다.
     * 예: dayOfMonth=31, 대상 월이 30일까지 → 30일 사용
     */
    private LocalDate adjustDayOfMonth(int year, int month, int desiredDay) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int lastDay = firstDay.lengthOfMonth();
        int safeDay = Math.min(desiredDay, lastDay);
        return LocalDate.of(year, month, safeDay);
    }
}
