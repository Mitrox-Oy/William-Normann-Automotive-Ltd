package com.ecommerse.backend.services.analytics;

import com.ecommerse.backend.dto.analytics.CreateDashboardRequest;
import com.ecommerse.backend.entities.analytics.AnalyticsDashboard;
import com.ecommerse.backend.repositories.analytics.AnalyticsDashboardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles CRUD operations for saved analytics dashboards.
 */
@Service
public class AnalyticsDashboardService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsDashboardService.class);

    private final AnalyticsDashboardRepository analyticsDashboardRepository;

    public AnalyticsDashboardService(AnalyticsDashboardRepository analyticsDashboardRepository) {
        this.analyticsDashboardRepository = analyticsDashboardRepository;
    }

    public AnalyticsDashboard createDashboard(CreateDashboardRequest request, Long ownerId) {
        AnalyticsDashboard dashboard = new AnalyticsDashboard();
        dashboard.setName(request.getName());
        dashboard.setDescription(request.getDescription());
        dashboard.setType(request.getType());
        dashboard.setConfigJson(request.getConfigJson());
        dashboard.setOwnerId(ownerId);
        dashboard.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : Boolean.FALSE);
        LOGGER.info("Creating analytics dashboard '{}' for owner {}", request.getName(), ownerId);
        return analyticsDashboardRepository.save(dashboard);
    }

    public List<AnalyticsDashboard> getUserDashboards(Long ownerId) {
        return analyticsDashboardRepository.findByOwnerIdOrIsPublicTrue(ownerId);
    }
}
