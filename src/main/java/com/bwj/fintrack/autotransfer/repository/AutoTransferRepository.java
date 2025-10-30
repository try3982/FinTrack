package com.bwj.fintrack.autotransfer.repository;

import com.bwj.fintrack.autotransfer.entity.AutoTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AutoTransferRepository extends JpaRepository<AutoTransfer, Long> {

    // 실행 대상 후보 찾기 위한 조회:
    // active = true, nextRunAt <= now, failCount <= maxRetries
    // (isDue()로 한 번 더 필터링할 거라 기본 조건만 잡아도 됨)
    List<AutoTransfer> findByActiveIsTrueAndNextRunAtLessThanEqual(LocalDateTime now);
}