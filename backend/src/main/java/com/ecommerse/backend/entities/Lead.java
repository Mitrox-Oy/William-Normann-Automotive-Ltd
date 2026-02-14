package com.ecommerse.backend.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lead_requests", indexes = {
        @Index(name = "idx_lead_requests_created_at", columnList = "created_at"),
        @Index(name = "idx_lead_requests_source", columnList = "source"),
        @Index(name = "idx_lead_requests_whatsapp_status", columnList = "whatsapp_status")
})
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(length = 60)
    private String phone;

    @Column(length = 120)
    private String interest;

    @Column(nullable = false, length = 4000)
    private String message;

    @Column(nullable = false)
    private Boolean consent = false;

    @Column(nullable = false, length = 80)
    private String source;

    @Column(length = 255)
    private String website;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "part_number", length = 120)
    private String partNumber;

    @Column(name = "requested_quantity", length = 30)
    private String requestedQuantity;

    @Column(name = "whatsapp_recipient", length = 30)
    private String whatsappRecipient;

    @Column(name = "whatsapp_message_id", length = 160)
    private String whatsappMessageId;

    @Column(name = "whatsapp_status", nullable = false, length = 30)
    private String whatsappStatus;

    @Column(name = "whatsapp_error", length = 2000)
    private String whatsappError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getInterest() {
        return interest;
    }

    public void setInterest(String interest) {
        this.interest = interest;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getConsent() {
        return consent;
    }

    public void setConsent(Boolean consent) {
        this.consent = consent;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(String requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public String getWhatsappRecipient() {
        return whatsappRecipient;
    }

    public void setWhatsappRecipient(String whatsappRecipient) {
        this.whatsappRecipient = whatsappRecipient;
    }

    public String getWhatsappMessageId() {
        return whatsappMessageId;
    }

    public void setWhatsappMessageId(String whatsappMessageId) {
        this.whatsappMessageId = whatsappMessageId;
    }

    public String getWhatsappStatus() {
        return whatsappStatus;
    }

    public void setWhatsappStatus(String whatsappStatus) {
        this.whatsappStatus = whatsappStatus;
    }

    public String getWhatsappError() {
        return whatsappError;
    }

    public void setWhatsappError(String whatsappError) {
        this.whatsappError = whatsappError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}