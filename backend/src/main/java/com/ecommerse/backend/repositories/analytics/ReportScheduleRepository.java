package com.ecommerse.backend.repositories.analytics;

import com.ecommerse.backend.entities.analytics.ReportSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, Long> {

    List<ReportSchedule> findByIsActiveTrue();

    List<ReportSchedule> findByNextRunAtBefore(LocalDateTime dateTime);
}
