package com.ecommerse.backend.dto;

import com.ecommerse.backend.entities.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating order status
 */
@Schema(description = "Request to update order status")
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "New order status", example = "SHIPPED", required = true)
    private OrderStatus status;

    public UpdateOrderStatusRequest() {
    }

    public UpdateOrderStatusRequest(OrderStatus status) {
        this.status = status;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}

