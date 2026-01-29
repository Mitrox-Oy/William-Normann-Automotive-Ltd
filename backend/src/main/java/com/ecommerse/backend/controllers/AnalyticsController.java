package com.ecommerse.backend.controllers;

import com.ecommerse.backend.dto.analytics.CohortAnalysisDTO;
import com.ecommerse.backend.dto.analytics.CreateDashboardRequest;
import com.ecommerse.backend.dto.analytics.DashboardMetricsDTO;
import com.ecommerse.backend.dto.analytics.ProductPerformanceDTO;
import com.ecommerse.backend.dto.analytics.StripePaidOrdersDashboardDTO;
import com.ecommerse.backend.dto.analytics.TimeSeriesDataDTO;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.entities.analytics.AnalyticsDashboard;
import com.ecommerse.backend.services.analytics.AnalyticsDashboardService;
import com.ecommerse.backend.services.analytics.MetricsService;
import com.ecommerse.backend.services.analytics.StripeOrderMonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints powering analytics dashboards for owners.
 */
@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AnalyticsController {

    private final MetricsService metricsService;
    private final AnalyticsDashboardService dashboardService;
    private final StripeOrderMonitoringService stripeOrderMonitoringService;

    public AnalyticsController(MetricsService metricsService,
                               AnalyticsDashboardService dashboardService,
                               StripeOrderMonitoringService stripeOrderMonitoringService) {
        this.metricsService = metricsService;
        this.dashboardService = dashboardService;
        this.stripeOrderMonitoringService = stripeOrderMonitoringService;
    }

    @GetMapping("/dashboard/executive")
    public ResponseEntity<DashboardMetricsDTO> getExecutiveDashboard(
            @RequestParam(defaultValue = "last_30_days") String dateRange) {
        try {
            DashboardMetricsDTO metrics = metricsService.getDashboardMetrics(dateRange, "EXECUTIVE");
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.ok(new DashboardMetricsDTO());
        }
    }

    @GetMapping("/dashboard/operational")
    public ResponseEntity<DashboardMetricsDTO> getOperationalDashboard(
            @RequestParam(defaultValue = "today") String dateRange) {
        try {
            DashboardMetricsDTO metrics = metricsService.getDashboardMetrics(dateRange, "OPERATIONAL");
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.ok(new DashboardMetricsDTO());
        }
    }

    @GetMapping("/dashboard/customer-analytics")
    public ResponseEntity<DashboardMetricsDTO> getCustomerAnalyticsDashboard(
            @RequestParam(defaultValue = "last_90_days") String dateRange) {
        try {
            DashboardMetricsDTO metrics = metricsService.getDashboardMetrics(dateRange, "CUSTOMER_ANALYTICS");
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.ok(new DashboardMetricsDTO());
        }
    }

    @GetMapping("/revenue-trend")
    public ResponseEntity<List<TimeSeriesDataDTO>> getRevenueTrend(
            @RequestParam(defaultValue = "last_30_days") String dateRange,
            @RequestParam(defaultValue = "daily") String granularity) {
        try {
            List<TimeSeriesDataDTO> trend = metricsService.getRevenueTrend(dateRange, granularity);
            return ResponseEntity.ok(trend);
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.List.of());
        }
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<ProductPerformanceDTO>> getTopProducts(
            @RequestParam(defaultValue = "last_30_days") String dateRange,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<ProductPerformanceDTO> products = metricsService.getTopProducts(dateRange, limit);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.List.of());
        }
    }

    @GetMapping("/cohort-analysis")
    public ResponseEntity<CohortAnalysisDTO> getCohortAnalysis(
            @RequestParam String cohortMonth) {
        try {
            CohortAnalysisDTO analysis = metricsService.getCohortAnalysis(cohortMonth);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.ok(new CohortAnalysisDTO());
        }
    }

    @PostMapping("/dashboards")
    public ResponseEntity<AnalyticsDashboard> createDashboard(
            @RequestBody CreateDashboardRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        AnalyticsDashboard dashboard = dashboardService.createDashboard(request, user.getId());
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/dashboards")
    public ResponseEntity<List<AnalyticsDashboard>> getUserDashboards(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<AnalyticsDashboard> dashboards = dashboardService.getUserDashboards(user.getId());
        return ResponseEntity.ok(dashboards);
    }

    @GetMapping("/stripe/paid-dashboard")
    public ResponseEntity<StripePaidOrdersDashboardDTO> getStripePaidOrdersDashboard(
            @RequestParam(defaultValue = "5") int limit) {
        try {
            StripePaidOrdersDashboardDTO dashboard = stripeOrderMonitoringService.getStripePaidOrdersDashboard(limit);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.ok(new StripePaidOrdersDashboardDTO());
        }
    }
}
