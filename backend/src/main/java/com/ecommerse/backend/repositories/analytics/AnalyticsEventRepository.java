package com.ecommerse.backend.repositories.analytics;

import com.ecommerse.backend.entities.analytics.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {

    List<AnalyticsEvent> findByOccurredAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("select e.type as type, count(e.id) as cnt from AnalyticsEvent e where e.occurredAt >= :from group by e.type")
    List<Object[]> countByTypeSince(LocalDateTime from);
}




