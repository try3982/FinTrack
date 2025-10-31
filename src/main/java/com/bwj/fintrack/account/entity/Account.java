package com.bwj.fintrack.account.entity;

import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import com.bwj.fintrack.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Pattern;


@Entity
@Table(
        name = "f_accounts",
        indexes = {
                @Index(name = "ix_accounts_user", columnList = "user_id"),
                @Index(name = "ux_accounts_no", columnList = "account_number", unique = true)
        }
)
@Getter
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    private static final Pattern ACCOUNT_NO_PATTERN = Pattern.compile("^\\d{3}-\\d{4}-\\d{7}$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_number", nullable = false, unique = true, length = 16)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private Boolean autoTransfer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountType accountType;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountStatus accountStatus;

    @Column(precision = 19, scale = 2)
    private BigDecimal minBalance;

    private LocalDateTime closedAt;

    private LocalDateTime restoreUntil;

    @Version
    private Long version;

    public static Account createActive(
            User user,
            String accountNumber,
            BigDecimal initialBalance,
            AccountType type,
            BigDecimal minBalance,
            boolean autoTransfer
    ) {
        Account a = new Account();
        a.user = user;
        a.accountNumber = accountNumber;
        a.balance = s2(initialBalance != null ? initialBalance : BigDecimal.ZERO);
        a.autoTransfer = autoTransfer;
        a.accountType = type;
        a.accountStatus = AccountStatus.ACTIVE;
        a.minBalance = minBalance != null ? s2(minBalance) : null;
        return a;
    }

    public static Account createDeposit(
            User user,
            String accountNumber,
            BigDecimal initialDeposit,
            boolean autoTransfer
    ) {
        // 초기 입금액 검증
        if (initialDeposit == null || initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException(ErrorCode.INVALID_INITIAL_DEPOSIT);
        }

        Account account = new Account();
        account.user = user;
        account.accountNumber = accountNumber;
        account.balance = s2(initialDeposit);
        account.autoTransfer = autoTransfer;
        account.accountType = AccountType.DEPOSIT;
        account.accountStatus = AccountStatus.ACTIVE;
        account.minBalance = null;  // 예금은 최소 잔액 제한 없음
        return account;
    }

    public static Account createSavings(
            User user,
            String accountNumber,
            BigDecimal initialDeposit,
            BigDecimal monthlyAmount
    ) {
        // 적금 최소 금액 상수
        BigDecimal minimumAmount = BigDecimal.valueOf(AccountType.SAVINGS.getMinimumInitial());

        // 초기 입금액 검증
        if (initialDeposit == null || initialDeposit.compareTo(minimumAmount) < 0) {
            throw new CustomException(ErrorCode.INVALID_INITIAL_DEPOSIT_FOR_SAVINGS);
        }

        // 월 납입액 검증
        if (monthlyAmount == null || monthlyAmount.compareTo(minimumAmount) < 0) {
            throw new CustomException(ErrorCode.INVALID_MONTHLY_AMOUNT);
        }

        Account account = new Account();
        account.user = user;
        account.accountNumber = accountNumber;
        account.balance = s2(initialDeposit);
        account.autoTransfer = true;  // 적금은 자동이체 필수
        account.accountType = AccountType.SAVINGS;
        account.accountStatus = AccountStatus.ACTIVE;
        account.minBalance = s2(monthlyAmount);  // 매월 납입을 위한 최소 잔액
        return account;
    }

    public void withdraw(BigDecimal amount) {
        // 1) 상태/입력 검증 (도메인 불변식)
        if (!isActive()) {
            throw new DomainRuleViolation(DomainRuleViolation.Reason.INACTIVE);
        }
        if (amount == null || amount.signum() <= 0) {
            throw new DomainRuleViolation(DomainRuleViolation.Reason.NON_POSITIVE_AMOUNT);
        }

        // 2) 출금 후 잔액 계산 (스케일 고정 포함)
        BigDecimal after = previewAfterWithdraw(amount); // == s2(this.balance - s2(amount))

        // 3) 정책 위반 검증
        if (wouldGoNegative(after)) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        if (wouldViolateMinBalance(after)) {
            throw new  CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 4) 상태 적용
        this.balance = after; // 최종 스케일은 previewAfterWithdraw가 보장
    }

    /**
     * 계좌 해지 도메인 규칙:
     * - 이미 CLOSED면 안 됨
     * - 잔액(balance)이 0.00이 아니면 안 됨
     * - 상태를 CLOSED로 전환
     * - closedAt 기록
     * - restoreUntil = 지금부터 3개월 뒤
     */
    public void closeAccount() {
        if (this.accountStatus == AccountStatus.CLOSED) {
            throw new CustomException(ErrorCode.ACCOUNT_ALREADY_CLOSED);
        }

        // 잔액 0이어야 해지 가능
        if (this.balance == null || this.balance.setScale(2, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new CustomException(ErrorCode.ACCOUNT_BALANCE_NOT_ZERO);
        }

        this.accountStatus = AccountStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
        this.restoreUntil = this.closedAt.plus(3, ChronoUnit.MONTHS);
    }



    public boolean isActive() {
        return this.accountStatus == AccountStatus.ACTIVE;
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }

    public static boolean isValidAccountNo(String no) {
        return no != null && ACCOUNT_NO_PATTERN.matcher(no).matches();
    }

    public BigDecimal previewAfterDeposit(BigDecimal amount) {
        return s2(this.balance.add(s2(amount)));
    }

    public BigDecimal previewAfterWithdraw(BigDecimal amount) {
        return s2(this.balance.subtract(s2(amount)));
    }

    public boolean wouldViolateMinBalance(BigDecimal after) {
        return this.minBalance != null && after.compareTo(this.minBalance) < 0;
    }

    public boolean wouldGoNegative(BigDecimal after) {
        return after.compareTo(BigDecimal.ZERO) < 0;
    }

    public void applyDeposit(BigDecimal amount) {
        this.balance = previewAfterDeposit(amount);
    }

    public void applyWithdraw(BigDecimal amount) {
        this.balance = previewAfterWithdraw(amount);
    }

    public void ensureActive() {
        if (accountStatus != AccountStatus.ACTIVE) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
    }

    public void deposit(BigDecimal amount) {
        if (!isActive()) throw new DomainRuleViolation(DomainRuleViolation.Reason.INACTIVE);
        if (amount == null || amount.signum() <= 0)
            throw new DomainRuleViolation(DomainRuleViolation.Reason.NON_POSITIVE_AMOUNT);

        BigDecimal normalized = s2(amount);           // 스케일 고정
        this.balance = s2(this.balance.add(normalized));
    }

    public class DomainRuleViolation extends RuntimeException {
        public enum Reason { INACTIVE, NON_POSITIVE_AMOUNT }
        private final Reason reason;
        public DomainRuleViolation(Reason reason) { this.reason = reason; }
        public Reason reason() { return reason; }
    }

    private static BigDecimal s2(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new CustomException(ErrorCode.AMOUNT_MUST_BE_POSITIVE);
        }
    }

    public void restoreAccount() {
        if (this.accountStatus != AccountStatus.CLOSED) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_CLOSED);
        }

        // 복원 기간 만료 검증
        if (this.restoreUntil == null || LocalDateTime.now().isAfter(this.restoreUntil)) {
            throw new CustomException(ErrorCode.ACCOUNT_RESTORE_EXPIRED);
        }

        this.accountStatus = AccountStatus.ACTIVE;
        this.closedAt = null;
        this.restoreUntil = null;
    }
}
