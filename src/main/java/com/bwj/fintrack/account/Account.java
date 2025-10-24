package com.bwj.fintrack.account;

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


    @Builder(builderMethodName = "of")
    public static Account create(User user,
                                 String accountNumber,
                                 BigDecimal balance,
                                 Boolean autoTransfer,
                                 AccountType accountType,
                                 AccountStatus accountStatus,
                                 BigDecimal minBalance) {
        validateAccountNo(accountNumber);
        Account a = new Account();
        a.user = user;
        a.accountNumber = accountNumber;
        a.balance = balance != null ? balance : BigDecimal.ZERO;
        a.autoTransfer = autoTransfer != null ? autoTransfer : Boolean.FALSE;
        a.accountType = accountType;
        a.accountStatus = accountStatus;
        a.minBalance = minBalance;
        return a;
    }

    public void ensureActive() {
        if (accountStatus != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account not active: " + accountStatus);
        }
    }

    public void deposit(BigDecimal amount) {
        ensureActive();
        requirePositive(amount);
        this.balance = s2(this.balance.add(s2(amount)));
    }


    public void withdraw(BigDecimal amount) {
        ensureActive();
        requirePositive(amount);
        BigDecimal after = s2(this.balance.subtract(s2(amount)));
        if (minBalance != null && after.compareTo(minBalance) < 0) {
            throw new IllegalStateException("Min balance violation");
        }
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance = after;
    }

    public void changeAccountNumber(String newNo) {
        validateAccountNo(newNo);
        this.accountNumber = newNo;
    }

    private static BigDecimal s2(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP); }

    private static void validateAccountNo(String no) {
        if (no == null || !ACCOUNT_NO_PATTERN.matcher(no).matches()) {
            throw new IllegalArgumentException("Invalid account number format. Expected ###-####-#######");
        }
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
    }
}
