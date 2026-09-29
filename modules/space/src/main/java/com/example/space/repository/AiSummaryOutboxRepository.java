package com.example.space.repository;

import com.example.space.entity.AiSummaryOutboxEvent;
import com.example.space.entity.AiSummaryOutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AiSummaryOutboxRepository extends JpaRepository<AiSummaryOutboxEvent, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from AiSummaryOutboxEvent e where "
            + "(e.status = :pending and e.nextAttemptAt <= :now) "
            + "or (e.status = :processing and e.lockedAt <= :staleBefore) "
            + "order by e.createdAt asc")
    List<AiSummaryOutboxEvent> findClaimable(
            @Param("pending") AiSummaryOutboxStatus pending,
            @Param("processing") AiSummaryOutboxStatus processing,
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore,
            Pageable pageable
    );
}
