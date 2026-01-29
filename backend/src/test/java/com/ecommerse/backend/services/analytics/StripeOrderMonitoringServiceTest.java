package com.ecommerse.backend.services.analytics;

import com.ecommerse.backend.dto.analytics.StripePaidOrdersDashboardDTO;
import com.ecommerse.backend.entities.Order;
import com.ecommerse.backend.entities.OrderStatus;
import com.ecommerse.backend.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeOrderMonitoringServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private StripeOrderMonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        monitoringService = new StripeOrderMonitoringService(orderRepository);
    }

    @Test
    void getStripePaidOrdersDashboardAggregatesMetrics() {
        when(orderRepository.countByStatusAndPaymentIntentIdIsNotNull(OrderStatus.PAID)).thenReturn(3L);
        when(orderRepository.countByStatusAndPaymentIntentIdIsNotNull(OrderStatus.CONFIRMED)).thenReturn(1L);
        when(orderRepository.getTotalSalesByStatusAndPaymentIntent(OrderStatus.PAID))
                .thenReturn(new BigDecimal("150.00"));

        Order first = buildOrder(1L, "ORD-1", "pi_1", new BigDecimal("100.00"), LocalDateTime.now().minusHours(2));
        Order second = buildOrder(2L, "ORD-2", "pi_2", new BigDecimal("50.00"), LocalDateTime.now().minusHours(1));
        Page<Order> page = new PageImpl<>(List.of(first, second));
        when(orderRepository.findByStatusAndPaymentIntentIdIsNotNullOrderByCreatedDateDesc(eq(OrderStatus.PAID), any(Pageable.class)))
                .thenReturn(page);

        StripePaidOrdersDashboardDTO dashboard = monitoringService.getStripePaidOrdersDashboard(3);

        verify(orderRepository).findByStatusAndPaymentIntentIdIsNotNullOrderByCreatedDateDesc(eq(OrderStatus.PAID), pageableCaptor.capture());
        assertEquals(3, pageableCaptor.getValue().getPageSize());

        assertEquals(3, dashboard.getTotalPaidOrders());
        assertEquals(1, dashboard.getPendingPaymentIntents());
        assertEquals(new BigDecimal("150.00"), dashboard.getTotalPaidAmount());
        assertEquals(new BigDecimal("50.00"), dashboard.getAverageOrderValue());
        assertEquals(2, dashboard.getRecentPaidOrders().size());
        assertNotNull(dashboard.getLastPaidAt());
        assertEquals("ORD-1", dashboard.getRecentPaidOrders().get(0).getOrderNumber());
    }

    @Test
    void getStripePaidOrdersDashboardUsesDefaultLimitWhenInvalid() {
        when(orderRepository.countByStatusAndPaymentIntentIdIsNotNull(OrderStatus.PAID)).thenReturn(0L);
        when(orderRepository.countByStatusAndPaymentIntentIdIsNotNull(OrderStatus.CONFIRMED)).thenReturn(0L);
        when(orderRepository.getTotalSalesByStatusAndPaymentIntent(OrderStatus.PAID))
                .thenReturn(BigDecimal.ZERO);
        when(orderRepository.findByStatusAndPaymentIntentIdIsNotNullOrderByCreatedDateDesc(eq(OrderStatus.PAID), any(Pageable.class)))
                .thenReturn(Page.empty());

        monitoringService.getStripePaidOrdersDashboard(-1);

        verify(orderRepository).findByStatusAndPaymentIntentIdIsNotNullOrderByCreatedDateDesc(eq(OrderStatus.PAID), pageableCaptor.capture());
        assertEquals(5, pageableCaptor.getValue().getPageSize());
    }

    private Order buildOrder(Long id, String orderNumber, String paymentIntent, BigDecimal amount, LocalDateTime updated) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNumber(orderNumber);
        order.setPaymentIntentId(paymentIntent);
        order.setStatus(OrderStatus.PAID);
        order.setTotalAmount(amount);
        order.setCurrency("eur");
        order.setCreatedDate(updated.minusHours(1));
        order.setUpdatedDate(updated);
        return order;
    }
}
