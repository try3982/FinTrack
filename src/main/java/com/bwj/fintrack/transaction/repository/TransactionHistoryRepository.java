package com.bwj.fintrack.transaction.repository;

import com.bwj.fintrack.transaction.entity.Transaction;
import com.bwj.fintrack.transaction.entity.TransactionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TransactionHistoryRepository {

    List<Transaction> findPageForAccount(
            Long accountId,
            Set<TransactionType> types,
            LocalDateTime from,
            LocalDateTime to,
            int limit,
            LocalDateTime cursorDate,
            UUID cursorId
    );


}