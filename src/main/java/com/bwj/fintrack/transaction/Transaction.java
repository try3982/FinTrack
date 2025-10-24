package com.bwj.fintrack.transaction;


import com.bwj.fintrack.account.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
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
}
