package com.ecommerse.backend.controllers;

import com.ecommerse.backend.entities.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderControllerAuthorizationTest {

    @Test
    void getOrdersForCurrentUserRequiresCustomerRole() throws NoSuchMethodException {
        Method method = OrderController.class.getMethod("getOrdersForCurrentUser", Authentication.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Expected @PreAuthorize on getOrdersForCurrentUser");
        assertEquals("hasRole('CUSTOMER')", preAuthorize.value());
    }

    @Test
    void getOrderByCheckoutSessionRequiresCustomerRole() throws NoSuchMethodException {
        Method method = OrderController.class.getMethod(
                "getOrderByCheckoutSession",
                String.class,
                Authentication.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Expected @PreAuthorize on getOrderByCheckoutSession");
        assertEquals("hasRole('CUSTOMER')", preAuthorize.value());
    }

    @Test
    void getLatestOrderForCurrentUserRequiresCustomerRole() throws NoSuchMethodException {
        Method method = OrderController.class.getMethod(
                "getLatestOrderForCurrentUser",
                Authentication.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Expected @PreAuthorize on getLatestOrderForCurrentUser");
        assertEquals("hasRole('CUSTOMER')", preAuthorize.value());
    }

    @Test
    void ownerOrdersEndpointRequiresOwnerOrAdminRole() throws NoSuchMethodException {
        Method method = OrderController.class.getMethod(
                "getOwnerOrders",
                int.class,
                int.class,
                OrderStatus.class,
                LocalDateTime.class,
                LocalDateTime.class,
                String.class
        );
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Expected @PreAuthorize on getOwnerOrders");
        assertEquals("hasAnyRole('OWNER', 'ADMIN')", preAuthorize.value());
    }
}
