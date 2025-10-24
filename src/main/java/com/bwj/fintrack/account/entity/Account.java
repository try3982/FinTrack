package com.bwj.fintrack.account.entity;

import com.bwj.fintrack.common.exception.custom.CustomException;
import com.bwj.fintrack.common.exception.response.ErrorCode;
import com.bwj.fintrack.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
    private static final int MONEY_SCALE = 2;

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

    public static Account createActiveUnchecked(
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
        a.balance = roundToCurrencyOrZero(initialBalance);
        a.autoTransfer = autoTransfer;
        a.accountType = type;
        a.accountStatus = AccountStatus.ACTIVE;
        a.minBalance = minBalance;
        return a;
    }

    public static boolean isValidAccountNo(String no) {
        return no != null && ACCOUNT_NO_PATTERN.matcher(no).matches();
    }


    public boolean isActive() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }

    public ErrorCode reasonDepositInvalid(BigDecimal amount) {
        if (!isActive()) return ErrorCode.ACCOUNT_NOT_ACTIVE;
        if (!isPositive(amount)) return ErrorCode.AMOUNT_MUST_BE_POSITIVE;
        return null;
    }

    public ErrorCode reasonWithdrawInvalid(BigDecimal amount) {
        if (!isActive()) return ErrorCode.ACCOUNT_NOT_ACTIVE;
        if (!isPositive(amount)) return ErrorCode.AMOUNT_MUST_BE_POSITIVE;

        BigDecimal after = roundToCurrency(this.balance.subtract(roundToCurrency(amount)));
        if (minBalance != null && after.compareTo(minBalance) < 0) return ErrorCode.MIN_BALANCE_VIOLATION;
        if (after.compareTo(BigDecimal.ZERO) < 0) return ErrorCode.INSUFFICIENT_BALANCE;
        return null;
    }

    public void applyDeposit(BigDecimal amount) {
        this.balance = roundToCurrency(this.balance.add(roundToCurrency(amount)));
    }

    public void applyWithdraw(BigDecimal amount) {
        BigDecimal after = roundToCurrency(this.balance.subtract(roundToCurrency(amount)));
        this.balance = after;
    }

    private static BigDecimal roundToCurrency(BigDecimal v) {
        return v.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal roundToCurrencyOrZero(BigDecimal v) {
        return (v == null) ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP) : roundToCurrency(v);
    }
}
