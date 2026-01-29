package com.ecommerse.backend.repositories.analytics;

import com.ecommerse.backend.entities.analytics.AnalyticsAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsAlertRepository extends JpaRepository<AnalyticsAlert, Long> {

    List<AnalyticsAlert> findByIsActiveTrue();
}
