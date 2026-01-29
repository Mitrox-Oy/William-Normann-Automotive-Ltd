package com.ecommerse.backend.services.notifications;

import com.ecommerse.backend.dto.OrderItemDTO;
import com.ecommerse.backend.entities.Order;
import com.ecommerse.backend.entities.OrderStatus;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.OrderRepository;
import com.ecommerse.backend.repositories.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Email service for sending notifications
 * Implements email sending using SMTP with Thymeleaf templates
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a");

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.from:noreply@example.com}")
    private String mailFrom;

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Autowired
    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine,
                       OrderRepository orderRepository, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    /**
     * Check if email is configured
     */
    private boolean isEmailConfigured() {
        return mailHost != null && !mailHost.trim().isEmpty();
    }

    /**
     * Send order confirmation email to customer
     *
     * @param orderId Order ID
     */
    public void sendOrderConfirmation(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

            User customer = order.getUser();
            if (customer == null) {
                logger.warn("Order {} has no associated user, skipping email", orderId);
                return;
            }

            String customerEmail = customer.getUsername(); // username is email
            String customerName = getCustomerName(customer);

            // Prepare template context
            Context context = new Context();
            context.setVariable("orderNumber", order.getOrderNumber());
            context.setVariable("orderDate", formatDate(order.getOrderDate()));
            context.setVariable("totalAmount", order.getTotalAmount());
            context.setVariable("customerName", customerName);
            context.setVariable("shippingAddress", formatShippingAddress(order));
            context.setVariable("orderItems", convertOrderItemsToDTO(order.getOrderItems()));

            // Render template
            String htmlContent = templateEngine.process("order-confirmation", context);

            // Send email
            sendEmail(customerEmail, "Order Confirmation - " + order.getOrderNumber(), htmlContent);

        } catch (Exception e) {
            logger.error("Failed to send order confirmation email for order {}: {}", orderId, e.getMessage(), e);
            // Don't throw - email failures shouldn't break order processing
        }
    }

    /**
     * Send notification email to owner when a new paid order is created
     *
     * @param orderId Order ID
     */
    public void sendOwnerNotification(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

            User customer = order.getUser();
            if (customer == null) {
                logger.warn("Order {} has no associated user, skipping owner notification", orderId);
                return;
            }

            // Find owner email (first OWNER or ADMIN user)
            String ownerEmail = findOwnerEmail();
            if (ownerEmail == null) {
                logger.warn("No owner email found, skipping owner notification for order {}", orderId);
                return;
            }

            String customerName = getCustomerName(customer);
            String customerEmail = customer.getUsername();

            // Prepare template context
            Context context = new Context();
            context.setVariable("orderNumber", order.getOrderNumber());
            context.setVariable("orderDate", formatDate(order.getOrderDate()));
            context.setVariable("totalAmount", order.getTotalAmount());
            context.setVariable("customerName", customerName);
            context.setVariable("customerEmail", customerEmail);
            context.setVariable("orderItems", convertOrderItemsToDTO(order.getOrderItems()));

            // Render template
            String htmlContent = templateEngine.process("owner-new-order", context);

            // Send email
            sendEmail(ownerEmail, "New Order Received - " + order.getOrderNumber(), htmlContent);

        } catch (Exception e) {
            logger.error("Failed to send owner notification email for order {}: {}", orderId, e.getMessage(), e);
            // Don't throw - email failures shouldn't break order processing
        }
    }

    /**
     * Send order status update email to customer
     *
     * @param orderId Order ID
     * @param status New order status
     */
    public void sendOrderStatusUpdate(Long orderId, String status) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

            User customer = order.getUser();
            if (customer == null) {
                logger.warn("Order {} has no associated user, skipping status update email", orderId);
                return;
            }

            String customerEmail = customer.getUsername();
            String customerName = getCustomerName(customer);

            // Map status to user-friendly message
            String statusMessage = getStatusMessage(status);

            // Prepare template context
            Context context = new Context();
            context.setVariable("orderNumber", order.getOrderNumber());
            context.setVariable("status", status);
            context.setVariable("statusMessage", statusMessage);
            context.setVariable("orderDate", formatDate(order.getOrderDate()));
            context.setVariable("totalAmount", order.getTotalAmount());
            context.setVariable("customerName", customerName);

            // Render template
            String htmlContent = templateEngine.process("order-status-update", context);

            // Send email
            sendEmail(customerEmail, "Order Status Update - " + order.getOrderNumber(), htmlContent);

        } catch (Exception e) {
            logger.error("Failed to send order status update email for order {}: {}", orderId, e.getMessage(), e);
            // Don't throw - email failures shouldn't break order processing
        }
    }

    /**
     * Send email using JavaMailSender or log to console in dev mode
     */
    private void sendEmail(String to, String subject, String htmlContent) {
        if (!isEmailConfigured()) {
            // Dev fallback: log email content to console
            logger.info("[EMAIL] To: {}, Subject: {}, Body: {}", to, subject, htmlContent);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);
            logger.info("Email sent successfully to {}: {}", to, subject);

        } catch (MessagingException e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            // Don't throw - email failures shouldn't break order processing
        }
    }

    /**
     * Find owner email (first OWNER or ADMIN user)
     */
    private String findOwnerEmail() {
        // Try OWNER first
        List<User> owners = userRepository.findByRole(User.Role.OWNER);
        if (!owners.isEmpty()) {
            return owners.get(0).getUsername(); // username is email
        }

        // Fallback to ADMIN
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        if (!admins.isEmpty()) {
            return admins.get(0).getUsername();
        }

        return null;
    }

    /**
     * Get customer display name
     */
    private String getCustomerName(User customer) {
        String firstName = customer.getFirstName();
        String lastName = customer.getLastName();
        if (firstName != null && !firstName.trim().isEmpty() && 
            lastName != null && !lastName.trim().isEmpty()) {
            return firstName + " " + lastName;
        }
        return customer.getUsername(); // Fallback to email
    }

    /**
     * Format shipping address
     */
    private String formatShippingAddress(Order order) {
        StringBuilder address = new StringBuilder();
        address.append(order.getShippingAddress());
        address.append("\n");
        address.append(order.getShippingCity());
        if (order.getShippingPostalCode() != null && !order.getShippingPostalCode().trim().isEmpty()) {
            address.append(", ");
            address.append(order.getShippingPostalCode());
        }
        address.append("\n");
        address.append(order.getShippingCountry());
        return address.toString();
    }

    /**
     * Format date for email
     */
    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(DATE_FORMATTER);
    }

    /**
     * Convert OrderItems to DTOs for template
     */
    private List<OrderItemDTO> convertOrderItemsToDTO(List<com.ecommerse.backend.entities.OrderItem> items) {
        return items.stream().map(item -> {
            OrderItemDTO dto = new OrderItemDTO();
            dto.setProductName(item.getProductName());
            dto.setProductSku(item.getProductSku());
            dto.setQuantity(item.getQuantity());
            dto.setUnitPrice(item.getUnitPrice());
            dto.setTotalPrice(item.getTotalPrice());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Get user-friendly status message
     */
    private String getStatusMessage(String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            switch (orderStatus) {
                case CONFIRMED:
                    return "Your order has been confirmed and is being processed.";
                case PROCESSING:
                    return "Your order is being prepared for shipment.";
                case SHIPPED:
                    return "Your order has been shipped and is on its way to you. You will receive tracking information soon.";
                case DELIVERED:
                    return "Your order has been delivered. Thank you for your purchase!";
                case CANCELLED:
                    return "Your order has been cancelled. If you have any questions, please contact our support team.";
                case REFUNDED:
                    return "Your order has been refunded. The refund will be processed according to your payment method.";
                default:
                    return "Your order status has been updated to: " + status;
            }
        } catch (IllegalArgumentException e) {
            return "Your order status has been updated to: " + status;
        }
    }
}
