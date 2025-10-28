package com.bwj.fintrack.autotransfer;


import com.bwj.fintrack.account.entity.Account;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "f_auto_transfers",
        indexes = @Index(name = "ix_auto_active_next", columnList = "active, next_run_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
public class AutoTransfer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    @Column(name = "to_account_no", nullable = false, length = 16)
    private String toAccountNo;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "day_of_month", nullable = false)
    private Integer dayOfMonth; // 1~31, 말일 보정은 서비스에서

    @Column(name = "run_time", nullable = false, length = 5) // "HH:mm"
    private String runTime;

    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime nextRunAt;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 3;

    @Version private Long version;
}
