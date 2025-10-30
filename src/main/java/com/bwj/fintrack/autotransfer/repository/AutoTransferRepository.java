package com.bwj.fintrack.autotransfer.repository;

import com.bwj.fintrack.autotransfer.entity.AutoTransfer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AutoTransferRepository extends JpaRepository<AutoTransfer, Long> {

    // 실행 대상 후보 찾기 위한 조회:
    // active = true, nextRunAt <= now, failCount <= maxRetries
    // (isDue()로 한 번 더 필터링할 거라 기본 조건만 잡아도 됨)
    List<AutoTransfer> findByActiveIsTrueAndNextRunAtLessThanEqual(LocalDateTime now);

    // 사용자 본인의 자동이체 규칙 목록 조회용
    List<AutoTransfer> findByFromAccount_User_IdOrderByNextRunAtAsc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a
        FROM AutoTransfer a
        WHERE a.active = true
          AND a.nextRunAt <= :now
    """)
    List<AutoTransfer> findDueTransfersForUpdate(LocalDateTime now);
}