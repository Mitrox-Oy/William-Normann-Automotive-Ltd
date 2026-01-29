package com.ecommerse.backend.repositories.analytics;

import com.ecommerse.backend.entities.analytics.FactSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface FactSessionRepository extends JpaRepository<FactSession, Long> {

    @Query("SELECT COUNT(f) FROM FactSession f WHERE f.startTime BETWEEN :start AND :end")
    Long getTotalSessions(@Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    @Query("SELECT (COUNT(CASE WHEN f.converted = true THEN 1 END) * 100.0 / NULLIF(COUNT(*), 0)) FROM FactSession f WHERE f.startTime BETWEEN :start AND :end")
    Double getConversionRate(@Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);
}
