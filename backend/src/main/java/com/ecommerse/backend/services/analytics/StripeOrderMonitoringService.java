package com.ecommerse.backend.services.analytics;

import com.ecommerse.backend.dto.analytics.StripePaidOrderSummaryDTO;
import com.ecommerse.backend.dto.analytics.StripePaidOrdersDashboardDTO;
import com.ecommerse.backend.entities.Order;
import com.ecommerse.backend.entities.OrderStatus;
import com.ecommerse.backend.repositories.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Provides Stripe order visibility metrics for owners and administrators.
 */
@Service
public class StripeOrderMonitoringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StripeOrderMonitoringService.class);
    private static final int MAX_RECENT_ORDERS = 50;

    private final OrderRepository orderRepository;

    public StripeOrderMonitoringService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public StripePaidOrdersDashboardDTO getStripePaidOrdersDashboard(int limit) {
        int normalizedLimit = limit <= 0 ? 5 : Math.min(limit, MAX_RECENT_ORDERS);
        LOGGER.debug("Fetching Stripe paid orders dashboard (limit={})", normalizedLimit);

        long paidCount = Optional.ofNullable(
                orderRepository.countByStatusAndPaymentIntentIdIsNotNull(OrderStatus.PAID))
                .orElse(0L);
        long pendingCount = Optional.ofNullable(
                orderRepository.countByStatusAndPaymentIntentIdIsNotNull(OrderStatus.CONFIRMED))
                .orElse(0L);
        BigDecimal totalAmount = Optional.ofNullable(
                orderRepository.getTotalSalesByStatusAndPaymentIntent(OrderStatus.PAID))
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal averageAmount = paidCount == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : totalAmount.divide(BigDecimal.valueOf(paidCount), 2, RoundingMode.HALF_UP);

        Page<Order> recentOrders = orderRepository
                .findByStatusAndPaymentIntentIdIsNotNullOrderByCreatedDateDesc(
                        OrderStatus.PAID, PageRequest.of(0, normalizedLimit));

        List<StripePaidOrderSummaryDTO> summaries = recentOrders.getContent().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        LocalDateTime lastPaidAt = summaries.stream()
                .map(StripePaidOrderSummaryDTO::getPaidAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        StripePaidOrdersDashboardDTO dashboard = new StripePaidOrdersDashboardDTO();
        dashboard.setTotalPaidOrders(paidCount);
        dashboard.setPendingPaymentIntents(pendingCount);
        dashboard.setTotalPaidAmount(totalAmount);
        dashboard.setAverageOrderValue(averageAmount);
        dashboard.setRecentPaidOrders(summaries);
        dashboard.setLastPaidAt(lastPaidAt);
        return dashboard;
    }

    private StripePaidOrderSummaryDTO toSummary(Order order) {
        StripePaidOrderSummaryDTO summary = new StripePaidOrderSummaryDTO();
        summary.setOrderId(order.getId());
        summary.setOrderNumber(order.getOrderNumber());
        summary.setTotalAmount(Optional.ofNullable(order.getTotalAmount()).orElse(BigDecimal.ZERO));
        summary.setCurrency(order.getCurrency());
        summary.setPaymentIntentId(order.getPaymentIntentId());
        summary.setStatus(order.getStatus());
        LocalDateTime paidAt = order.getUpdatedDate() != null ? order.getUpdatedDate() : order.getCreatedDate();
        summary.setPaidAt(paidAt);
        return summary;
    }
}
