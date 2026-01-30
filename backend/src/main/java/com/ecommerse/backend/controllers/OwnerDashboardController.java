package com.ecommerse.backend.controllers;

import com.ecommerse.backend.dto.OrderDTO;
import com.ecommerse.backend.entities.OrderStatus;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.OrderRepository;
import com.ecommerse.backend.repositories.ProductRepository;
import com.ecommerse.backend.repositories.UserRepository;
import com.ecommerse.backend.services.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class OwnerDashboardController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;

    public OwnerDashboardController(OrderRepository orderRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            OrderService orderService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderService = orderService;
    }

    public record ProductSummary(Long id, String name, String sku, Integer stockLevel) {
    }

    public record DashboardStatsResponse(
            BigDecimal totalRevenue,
            long totalOrders,
            long totalCustomers,
            long totalProducts,
            List<OrderDTO> recentOrders,
            List<ProductSummary> lowStockProducts,
            List<Object> revenueByMonth) {
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        BigDecimal totalRevenue = orderRepository.getTotalSalesByStatusAndPaymentIntent(OrderStatus.PAID);
        if (totalRevenue == null)
            totalRevenue = BigDecimal.ZERO;

        long totalOrders = orderRepository.count();
        long totalCustomers = userRepository.findByRole(User.Role.CUSTOMER).size();
        long totalProducts = productRepository.count();

        List<OrderDTO> recentOrders = orderService.getRecentOrdersForOwner(5);

        List<ProductSummary> lowStockProducts = productRepository
                .findByActiveTrueAndStockQuantityLessThanOrderByStockQuantityAsc(5)
                .stream()
                .map(p -> new ProductSummary(p.getId(), p.getName(), p.getSku(), p.getStockQuantity()))
                .collect(java.util.stream.Collectors.toList());

        DashboardStatsResponse response = new DashboardStatsResponse(
                totalRevenue,
                totalOrders,
                totalCustomers,
                totalProducts,
                recentOrders,
                lowStockProducts,
                List.of());

        return ResponseEntity.ok(response);
    }
}
