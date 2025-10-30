package com.bwj.fintrack.transaction.entity;


import com.bwj.fintrack.account.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "f_transactions",
        indexes = @Index(name = "ix_tx_account_time", columnList = "account_id, transaction_date"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private TransactionResultType transactionResultType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private TransactionMethodType transactionMethodType;

    @CreatedDate
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceSnapshot;

    @Column(precision = 19, scale = 2)
    private BigDecimal fee;

    @Column(length = 200)
    private String memo;

    @PrePersist
    void prePersist() {
        if (transactionDate == null) transactionDate = LocalDateTime.now();
    }

    public static Transaction depositSuccess(Account account,
                                             BigDecimal amount,
                                             TransactionMethodType method,
                                             String memo) {
        return Transaction.builder()
                .account(account)
                .transactionType(TransactionType.DEPOSIT)
                .transactionResultType(TransactionResultType.SUCCESS)
                .transactionMethodType(method != null ? method : TransactionMethodType.ONLINE)
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .balanceSnapshot(account.getBalance())
                .fee(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .memo(memo)
                .build();
    }

    public static Transaction withdrawalSuccess(Account account,
                                                BigDecimal amount,
                                                TransactionMethodType method,
                                                String memo) {
        return Transaction.builder()
                .account(account)
                .transactionType(TransactionType.WITHDRAWAL)
                .transactionResultType(TransactionResultType.SUCCESS)
                .transactionMethodType(method != null ? method : TransactionMethodType.ONLINE)
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .balanceSnapshot(account.getBalance())
                .fee(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .memo(memo)
                .build();
    }

    public static Transaction transferOutSuccess(
            com.bwj.fintrack.account.entity.Account from,
            java.math.BigDecimal amount,
            TransactionMethodType method,
            String memo
    ) {
        return Transaction.builder()
                .account(from)
                .transactionType(TransactionType.TRANSFER_OUT)
                .transactionResultType(TransactionResultType.SUCCESS)
                .transactionMethodType(method != null ? method : TransactionMethodType.ONLINE)
                .amount(amount.setScale(2, java.math.RoundingMode.HALF_UP))
                .balanceSnapshot(from.getBalance()) // 출금 반영 후 잔액
                .fee(java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP))
                .memo(memo)
                .build();
    }

    public static Transaction transferInSuccess(
            com.bwj.fintrack.account.entity.Account to,
            java.math.BigDecimal amount,
            TransactionMethodType method,
            String memo
    ) {
        return Transaction.builder()
                .account(to)
                .transactionType(TransactionType.TRANSFER_IN)
                .transactionResultType(TransactionResultType.SUCCESS)
                .transactionMethodType(method != null ? method : TransactionMethodType.ONLINE)
                .amount(amount.setScale(2, java.math.RoundingMode.HALF_UP))
                .balanceSnapshot(to.getBalance()) // 입금 반영 후 잔액
                .fee(java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP))
                .memo(memo)
                .build();
    }


}
