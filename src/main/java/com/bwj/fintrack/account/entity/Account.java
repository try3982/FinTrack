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
}
