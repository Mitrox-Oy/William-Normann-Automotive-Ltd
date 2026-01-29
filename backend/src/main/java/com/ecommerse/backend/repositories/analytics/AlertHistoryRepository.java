package com.ecommerse.backend.repositories.analytics;

import com.ecommerse.backend.entities.analytics.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {

    List<AlertHistory> findByAlertIdOrderByTriggeredAtDesc(Long alertId);

    List<AlertHistory> findByAcknowledgedFalseOrderByTriggeredAtDesc();

    List<AlertHistory> findByAcknowledgedTrueOrderByTriggeredAtDesc();
}
