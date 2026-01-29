package com.ecommerse.backend.dto;

public record CreateOrderResponse(Long orderId, String orderNumber, long totalCents, String currency) {}


