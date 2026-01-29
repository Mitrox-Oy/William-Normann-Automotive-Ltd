package com.ecommerse.backend.services.analytics;

import com.ecommerse.backend.dto.analytics.CohortAnalysisDTO;
import com.ecommerse.backend.dto.analytics.DashboardMetricsDTO;
import com.ecommerse.backend.dto.analytics.PeriodComparisonDTO;
import com.ecommerse.backend.dto.analytics.ProductPerformanceDTO;
import com.ecommerse.backend.dto.analytics.TimeSeriesDataDTO;
import com.ecommerse.backend.dto.analytics.TrafficHealthDTO;
import com.ecommerse.backend.entities.Product;
import com.ecommerse.backend.entities.analytics.FactCustomerMetrics;
import com.ecommerse.backend.repositories.OrderRepository;
import com.ecommerse.backend.repositories.ProductRepository;
import com.ecommerse.backend.repositories.analytics.FactCustomerMetricsRepository;
import com.ecommerse.backend.repositories.analytics.FactOrderRepository;
import com.ecommerse.backend.repositories.analytics.FactSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Provides analytics aggregates and derived insights for dashboards and reports.
 */
@Service
public class MetricsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsService.class);

    private final FactOrderRepository factOrderRepository;
    private final FactSessionRepository factSessionRepository;
    private final FactCustomerMetricsRepository customerMetricsRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public MetricsService(FactOrderRepository factOrderRepository,
                          FactSessionRepository factSessionRepository,
                          FactCustomerMetricsRepository customerMetricsRepository,
                          OrderRepository orderRepository,
                          ProductRepository productRepository) {
        this.factOrderRepository = factOrderRepository;
        this.factSessionRepository = factSessionRepository;
        this.customerMetricsRepository = customerMetricsRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Cacheable(value = "dashboardMetrics", key = "#dateRange + '-' + #type")
    public DashboardMetricsDTO getDashboardMetrics(String dateRange, String type) {
        DateRange range = parseDateRange(dateRange);
        LOGGER.debug("Fetching dashboard metrics for type {} and range {}", type, dateRange);

        return switch (type == null ? "" : type.toUpperCase()) {
            case "EXECUTIVE" -> getExecutiveMetrics(range);
            case "OPERATIONAL" -> getOperationalMetrics(range);
            case "CUSTOMER_ANALYTICS" -> getCustomerAnalyticsMetrics(range);
            default -> getExecutiveMetrics(range);
        };
    }

    public List<TimeSeriesDataDTO> getRevenueTrend(String dateRange, String granularity) {
        DateRange range = parseDateRange(dateRange);
        List<Object[]> results = factOrderRepository.getDailyRevenue(range.startDate, range.endDate);
        List<TimeSeriesDataDTO> series = new ArrayList<>();
        results.forEach(row -> series.add(new TimeSeriesDataDTO((LocalDate) row[0], toBigDecimal(row[1]))));
        return series;
    }

    public List<ProductPerformanceDTO> getTopProducts(String dateRange, int limit) {
        DateRange range = parseDateRange(dateRange);
        List<Object[]> results = factOrderRepository.getRevenueByProduct(range.startDate, range.endDate);
        List<ProductPerformanceDTO> products = new ArrayList<>();

        for (Object[] row : results) {
            Long productId = (Long) row[0];
            BigDecimal revenue = toBigDecimal(row[1]);
            Long quantity = row[2] != null ? ((Number) row[2]).longValue() : 0L;

            ProductPerformanceDTO dto = new ProductPerformanceDTO();
            dto.setProductId(productId);
            dto.setRevenue(revenue);
            dto.setQuantity(quantity);
            dto.setGrowthRate(0.0); // Placeholder until prior-period logic is added

            Optional<Product> product = productRepository.findById(productId);
            dto.setProductName(product.map(Product::getName).orElse("Product " + productId));

            products.add(dto);
            if (products.size() >= limit) {
                break;
            }
        }
        return products;
    }

    public CohortAnalysisDTO getCohortAnalysis(String cohortMonth) {
        List<FactCustomerMetrics> cohortMetrics = customerMetricsRepository.findByCohortMonth(cohortMonth);
        if (cohortMetrics.isEmpty()) {
            CohortAnalysisDTO dto = new CohortAnalysisDTO();
            dto.setCohortMonth(cohortMonth);
            dto.setAverageLifetimeValue(BigDecimal.ZERO);
            dto.setRetentionRate(0.0);
            dto.setActiveCustomers(0);
            dto.setRetentionByPeriod(Collections.emptyMap());
            return dto;
        }

        BigDecimal totalLtv = BigDecimal.ZERO;
        int totalCustomers = cohortMetrics.size();
        int activeCustomers = 0;
        Map<Integer, Double> retention = new HashMap<>();

        for (FactCustomerMetrics metrics : cohortMetrics) {
            totalLtv = totalLtv.add(Optional.ofNullable(metrics.getLifetimeValue()).orElse(BigDecimal.ZERO));
            if (metrics.getDaysSinceLastOrder() != null && metrics.getDaysSinceLastOrder() <= 30) {
                activeCustomers++;
            }
        }

        CohortAnalysisDTO dto = new CohortAnalysisDTO();
        dto.setCohortMonth(cohortMonth);
        dto.setAverageLifetimeValue(totalCustomers == 0 ? BigDecimal.ZERO : totalLtv.divide(BigDecimal.valueOf(totalCustomers), 2, RoundingMode.HALF_UP));
        dto.setActiveCustomers(activeCustomers);
        dto.setRetentionRate(totalCustomers == 0 ? 0.0 : (activeCustomers * 100.0) / totalCustomers);
        dto.setRetentionByPeriod(retention);
        return dto;
    }

    private DashboardMetricsDTO getExecutiveMetrics(DateRange range) {
        DashboardMetricsDTO metrics = new DashboardMetricsDTO();

        BigDecimal totalRevenue = Optional.ofNullable(factOrderRepository.getTotalRevenue(range.startDate, range.endDate)).orElse(BigDecimal.ZERO);
        Long totalUnits = Optional.ofNullable(factOrderRepository.getTotalUnits(range.startDate, range.endDate)).orElse(0L);
        long totalOrders = orderRepository.countByCreatedDateBetween(range.startDateTime, range.endDateTime);

        metrics.setTotalRevenue(totalRevenue);
        metrics.setTotalOrders(totalOrders);
        metrics.setAverageOrderValue(totalOrders == 0 ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP));
        metrics.setTotalCost(BigDecimal.ZERO);
        metrics.setProfit(totalRevenue);

        Double conversionRate = Optional.ofNullable(factSessionRepository.getConversionRate(range.startDateTime, range.endDateTime)).orElse(0.0);
        metrics.setConversionRate(conversionRate);

        TrafficHealthDTO traffic = calculateTrafficHealth(range);
        metrics.setTrafficHealth(traffic);

        PeriodComparisonDTO comparison = calculateComparison(range, totalRevenue, conversionRate, traffic.getTotalSessions(), metrics.getAverageOrderValue());
        metrics.setComparison(comparison);

        return metrics;
    }

    private DashboardMetricsDTO getOperationalMetrics(DateRange range) {
        // For now reuse executive metrics, but this is the hook for fulfillment SLAs, etc.
        return getExecutiveMetrics(range);
    }

    private DashboardMetricsDTO getCustomerAnalyticsMetrics(DateRange range) {
        DashboardMetricsDTO metrics = getExecutiveMetrics(range);
        BigDecimal averageLtv = Optional.ofNullable(customerMetricsRepository.getAverageLifetimeValue(range.endDate)).orElse(BigDecimal.ZERO);
        metrics.setAverageOrderValue(averageLtv);
        return metrics;
    }

    private TrafficHealthDTO calculateTrafficHealth(DateRange range) {
        TrafficHealthDTO traffic = new TrafficHealthDTO();
        Long sessions = Optional.ofNullable(factSessionRepository.getTotalSessions(range.startDateTime, range.endDateTime)).orElse(0L);
        traffic.setTotalSessions(sessions);
        traffic.setProductViews(0L);
        traffic.setCartActions(0L);

        Double conversionRate = Optional.ofNullable(factSessionRepository.getConversionRate(range.startDateTime, range.endDateTime)).orElse(0.0);
        long convertedSessions = Math.round((conversionRate / 100.0) * sessions);
        traffic.setConvertedSessions(convertedSessions);
        traffic.setBounceRate(0.0);
        return traffic;
    }

    private PeriodComparisonDTO calculateComparison(DateRange currentRange,
                                                    BigDecimal currentRevenue,
                                                    Double currentConversionRate,
                                                    Long currentSessions,
                                                    BigDecimal currentAov) {
        if (currentRange.previous == null) {
            return new PeriodComparisonDTO();
        }
        DateRange previous = currentRange.previous;

        BigDecimal previousRevenue = Optional.ofNullable(factOrderRepository.getTotalRevenue(previous.startDate, previous.endDate)).orElse(BigDecimal.ZERO);
        Double previousConversion = Optional.ofNullable(factSessionRepository.getConversionRate(previous.startDateTime, previous.endDateTime)).orElse(0.0);
        Long previousSessions = Optional.ofNullable(factSessionRepository.getTotalSessions(previous.startDateTime, previous.endDateTime)).orElse(0L);

        long previousOrders = orderRepository.countByCreatedDateBetween(previous.startDateTime, previous.endDateTime);
        BigDecimal previousAov = previousOrders == 0 ? BigDecimal.ZERO
                : Optional.ofNullable(factOrderRepository.getTotalRevenue(previous.startDate, previous.endDate)).orElse(BigDecimal.ZERO)
                .divide(BigDecimal.valueOf(previousOrders), 2, RoundingMode.HALF_UP);

        PeriodComparisonDTO dto = new PeriodComparisonDTO();
        dto.setRevenueGrowth(calculateGrowth(currentRevenue, previousRevenue));
        dto.setConversionGrowth(calculateGrowth(currentConversionRate, previousConversion));
        dto.setSessionGrowth(calculateGrowth(Double.valueOf(currentSessions), Double.valueOf(previousSessions)));
        dto.setAovGrowth(calculateGrowth(currentAov, previousAov));
        return dto;
    }

    private double calculateGrowth(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private double calculateGrowth(Double current, Double previous) {
        if (previous == null || previous == 0.0) {
            return current != null && current > 0 ? 100.0 : 0.0;
        }
        return ((current - previous) / previous) * 100.0;
    }

    private DateRange parseDateRange(String rangeKey) {
        String key = rangeKey == null ? "last_30_days" : rangeKey.toLowerCase();
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;

        switch (key) {
            case "today" -> {
                start = today;
                end = today;
            }
            case "yesterday" -> {
                start = today.minusDays(1);
                end = today.minusDays(1);
            }
            case "last_7_days" -> {
                start = today.minusDays(6);
                end = today;
            }
            case "last_90_days" -> {
                start = today.minusDays(89);
                end = today;
            }
            case "this_month" -> {
                YearMonth ym = YearMonth.from(today);
                start = ym.atDay(1);
                end = today;
            }
            case "last_month" -> {
                YearMonth ym = YearMonth.from(today).minusMonths(1);
                start = ym.atDay(1);
                end = ym.atEndOfMonth();
            }
            case "last_30_days" -> {
                start = today.minusDays(29);
                end = today;
            }
            default -> {
                start = today.minusDays(29);
                end = today;
            }
        }

        DateRange current = new DateRange(start, end);
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate previousEnd = start.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(days - 1);
        current.previous = new DateRange(previousStart, previousEnd);
        return current;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private static class DateRange {
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final LocalDateTime startDateTime;
        private final LocalDateTime endDateTime;
        private DateRange previous;

        private DateRange(LocalDate start, LocalDate end) {
            this.startDate = start;
            this.endDate = end;
            this.startDateTime = start.atStartOfDay();
            this.endDateTime = end.atTime(23, 59, 59);
        }
    }
}
