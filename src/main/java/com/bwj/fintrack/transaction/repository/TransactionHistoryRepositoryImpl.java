package com.bwj.fintrack.transaction.repository;

import com.bwj.fintrack.transaction.entity.Transaction;
import com.bwj.fintrack.transaction.entity.TransactionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class TransactionHistoryRepositoryImpl implements TransactionHistoryRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * DESC 정렬 (최신 거래 먼저)
     * cursorDate/cursorId 이후의 레코드만 가져오는 keyset pagination
     */
    @Override
    public List<Transaction> findPageForAccount(
            Long accountId,
            Set<TransactionType> types,
            LocalDateTime from,
            LocalDateTime to,
            int limit,
            LocalDateTime cursorDate,
            UUID cursorId
    ) {

        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(Transaction.class);
        Root<Transaction> tx = cq.from(Transaction.class);

        List<Predicate> predicates = new ArrayList<>();

        // 내 계좌만
        predicates.add(cb.equal(tx.get("account").get("id"), accountId));

        // 기간 필터(from ~ to)
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(tx.get("transactionDate"), from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(tx.get("transactionDate"), to));
        }

        // 거래 유형 필터 (DEPOSIT / WITHDRAWAL / TRANSFER_IN / TRANSFER_OUT)
        if (types != null && !types.isEmpty()) {
            predicates.add(tx.get("transactionType").in(types));
        }

        // 커서 필터 (DESC이므로 "이 커서보다 더 '과거'인 것만")
        if (cursorDate != null && cursorId != null) {
            Predicate earlierDate = cb.lessThan(tx.get("transactionDate"), cursorDate);
            Predicate sameDateLowerId = cb.and(
                    cb.equal(tx.get("transactionDate"), cursorDate),
                    cb.lessThan(tx.get("id"), cursorId)
            );
            predicates.add(cb.or(earlierDate, sameDateLowerId));
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));

        // 정렬: 최신순 (transactionDate DESC, id DESC)
        List<Order> orders = new ArrayList<>();
        orders.add(cb.desc(tx.get("transactionDate")));
        orders.add(cb.desc(tx.get("id")));
        cq.orderBy(orders);

        return em.createQuery(cq)
                .setMaxResults(limit)
                .getResultList();
    }
}