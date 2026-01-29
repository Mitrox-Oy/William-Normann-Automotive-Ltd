package com.ecommerse.backend.repositories.analytics;

import com.ecommerse.backend.entities.analytics.AnalyticsDashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsDashboardRepository extends JpaRepository<AnalyticsDashboard, Long> {

    List<AnalyticsDashboard> findByOwnerIdOrIsPublicTrue(Long ownerId);

    List<AnalyticsDashboard> findByType(String type);
}
