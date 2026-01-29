package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.OrderDTO;
import com.ecommerse.backend.entities.Category;
import com.ecommerse.backend.entities.Order;
import com.ecommerse.backend.entities.OrderItem;
import com.ecommerse.backend.entities.OrderStatus;
import com.ecommerse.backend.entities.Product;
import com.ecommerse.backend.repositories.CartRepository;
import com.ecommerse.backend.repositories.OrderRepository;
import com.ecommerse.backend.repositories.ProductRepository;
import com.ecommerse.backend.repositories.UserRepository;
import com.ecommerse.backend.services.analytics.AlertService;
import com.ecommerse.backend.services.notifications.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceStripeTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartService cartService;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private AlertService alertService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    @InjectMocks
    private OrderService orderService;

    private Order createOrderWithItem(int quantity, int stockQuantity, boolean inventoryLocked) {
        Order order = new Order();
        order.setId(42L);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        order.setShippingAddress("123 Main St");
        order.setShippingCity("City");
        order.setShippingPostalCode("12345");
        order.setShippingCountry("Country");

        Category category = new Category();
        category.setName("Electronics");
        category.setSlug("electronics");

        Product product = new Product();
        product.setId(7L);
        product.setName("Gaming Console");
        product.setSku("SKU-123");
        product.setPrice(new BigDecimal("499.99"));
        product.setStockQuantity(stockQuantity);
        product.setCategory(category);

        OrderItem orderItem = new OrderItem(order, product, quantity);
        order.addOrderItem(orderItem);
        order.setTotalAmount(orderItem.getTotalPrice());
        order.setCreatedDate(LocalDateTime.now());
        order.setInventoryLocked(inventoryLocked);
        return order;
    }

    @Test
    void markOrderAsPaidFromWebhookShouldDecrementStockWhenInventoryNotLocked() {
        Order order = createOrderWithItem(2, 5, false);
        when(orderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDTO dto = orderService.markOrderAsPaidFromWebhook(42L, "pi_test");

        assertEquals(OrderStatus.PAID, dto.getStatus());
        verify(productRepository).save(productCaptor.capture());
        assertEquals(3, productCaptor.getValue().getStockQuantity());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(emailService).sendOrderConfirmation(42L);
        verify(emailService).sendOwnerNotification(42L);
        verify(alertService).recordSystemAlert(eq("Order paid: " + order.getOrderNumber()), any());
    }

    @Test
    void markOrderAsPaidFromWebhookShouldNotPropagateNotificationFailures() {
        Order order = createOrderWithItem(1, 2, false);
        when(orderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new RuntimeException("mail down"))
                .when(emailService).sendOrderConfirmation(anyLong());
        doThrow(new RuntimeException("owner mail down"))
                .when(emailService).sendOwnerNotification(anyLong());
        doThrow(new RuntimeException("alert down"))
                .when(alertService).recordSystemAlert(any(), any());

        assertDoesNotThrow(() -> orderService.markOrderAsPaidFromWebhook(42L, "pi_test"));

        verify(emailService).sendOrderConfirmation(42L);
        verify(emailService).sendOwnerNotification(42L);
        verify(alertService).recordSystemAlert(eq("Order paid: " + order.getOrderNumber()), any());
        verify(productRepository).save(productCaptor.capture());
        assertEquals(1, productCaptor.getValue().getStockQuantity());
    }

    @Test
    void markOrderAsPaidFromWebhookRejectsWhenStockInsufficientAndNotLocked() {
        Order order = createOrderWithItem(5, 2, false);
        when(orderRepository.findById(42L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class,
                () -> orderService.markOrderAsPaidFromWebhook(42L, "pi_test"));

        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void markOrderAsPaidFromWebhookDoesNotAdjustStockWhenInventoryLocked() {
        Order order = createOrderWithItem(2, 0, true);
        when(orderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDTO dto = orderService.markOrderAsPaidFromWebhook(42L, "pi_test");

        assertEquals(OrderStatus.PAID, dto.getStatus());
        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void markPaymentFailedRestoresInventoryWhenLocked() {
        Order order = createOrderWithItem(3, 0, true);
        order.setStatus(OrderStatus.CHECKOUT_CREATED);
        when(orderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDTO dto = orderService.markPaymentFailed("42", "pi_test");

        assertEquals(OrderStatus.FAILED, dto.getStatus());
        verify(productRepository).save(productCaptor.capture());
        assertEquals(3, productCaptor.getValue().getStockQuantity());
    }
}
