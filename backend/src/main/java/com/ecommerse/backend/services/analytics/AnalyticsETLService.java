package com.ecommerse.backend.services.analytics;

import com.ecommerse.backend.entities.Order;
import com.ecommerse.backend.entities.OrderItem;
import com.ecommerse.backend.entities.OrderStatus;
import com.ecommerse.backend.entities.analytics.FactOrder;
import com.ecommerse.backend.repositories.OrderRepository;
import com.ecommerse.backend.repositories.analytics.FactOrderRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Extract-transform-load pipeline that snapshots operational order data into analytics fact tables.
 */
@Service
public class AnalyticsETLService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsETLService.class);
    private static final DateTimeFormatter DAY_KEY_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final OrderRepository orderRepository;
    private final FactOrderRepository factOrderRepository;

    public AnalyticsETLService(OrderRepository orderRepository,
                               FactOrderRepository factOrderRepository) {
        this.orderRepository = orderRepository;
        this.factOrderRepository = factOrderRepository;
    }

    /**
     * Daily ETL (runs at 1:00am server time) processing previous day orders.
     */
    @Transactional
    @Scheduled(cron = "0 0 1 * * ?")
    public void runDailyETL() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        LOGGER.info("Starting scheduled analytics ETL for {}", targetDate);
        processOrdersForDate(targetDate);
    }

    /**
     * Manual trigger for a specific date, useful for backfills.
     */
    @Transactional
    public void processOrdersForDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);

        List<Order> orders = orderRepository.findByCreatedDateBetween(start, end);
        LOGGER.info("Found {} orders to process for {}", orders.size(), date);

        orders.forEach(this::upsertFactOrders);
    }

    /**
     * Backfill a range of historical dates.
     */
    @Transactional
    public void backfill(LocalDate startDate, LocalDate endDate) {
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            processOrdersForDate(current);
            current = current.plusDays(1);
        }
    }

    private void upsertFactOrders(Order order) {
        order.getOrderItems().forEach(item -> {
            FactOrder factOrder = factOrderRepository.findByOrderIdAndProductId(order.getId(), item.getProduct().getId())
                    .orElseGet(FactOrder::new);

            factOrder.setOrderId(order.getId());
            factOrder.setCustomerId(order.getUser() != null ? order.getUser().getId() : null);
            factOrder.setProductId(item.getProduct().getId());
            factOrder.setCategoryId(item.getProduct().getCategory() != null ? item.getProduct().getCategory().getId() : null);

            LocalDate orderDate = order.getCreatedDate().toLocalDate();
            factOrder.setOrderDate(orderDate);
            factOrder.setYearKey(orderDate.getYear());
            factOrder.setMonthKey(orderDate.getYear() * 100 + orderDate.getMonthValue());
            factOrder.setDayKey(Integer.parseInt(orderDate.format(DAY_KEY_FORMAT)));

            BigDecimal revenue = item.getTotalPrice();
            factOrder.setRevenue(revenue);
            factOrder.setCost(BigDecimal.ZERO);
            factOrder.setProfit(revenue); // Without cost data, assume full revenue as profit
            factOrder.setQuantity(item.getQuantity());
            factOrder.setDiscountAmount(BigDecimal.ZERO);
            factOrder.setIsCancelled(order.getStatus() == OrderStatus.CANCELLED);
            factOrder.setIsReturned(Boolean.FALSE);

            factOrderRepository.save(factOrder);
        });
    }
}
