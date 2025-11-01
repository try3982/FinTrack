package com.bwj.fintrack.account.repository;

import com.bwj.fintrack.account.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findById(Long id);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :accountNumber")
    Optional<Account> findWithLockByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findAllByUser_IdOrderByIdDesc(Long userId);

    @Query("""
        SELECT COALESCE(SUM(a.balance), 0)
        FROM Account a
        WHERE a.user.id = :userId
          AND a.accountStatus = com.bwj.fintrack.account.entity.AccountStatus.ACTIVE
    """)
    BigDecimal sumActiveBalanceByUserId(@Param("userId") Long userId);

}
