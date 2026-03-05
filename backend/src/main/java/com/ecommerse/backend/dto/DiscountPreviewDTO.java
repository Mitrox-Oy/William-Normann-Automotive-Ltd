package com.ecommerse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Discount preview result for a cart")
public class DiscountPreviewDTO {

    @Schema(description = "Whether the provided code is valid and applied", example = "true")
    private Boolean valid;

    @Schema(description = "User-friendly status message", example = "Discount code applied")
    private String message;

    @Schema(description = "Normalized discount code", example = "SAVE10")
    private String code;

    @Schema(description = "Discount percentage", example = "10.00")
    private BigDecimal percentage;

    @Schema(description = "Subtotal after product-level sales but before coupon", example = "1250.00")
    private BigDecimal subtotal;

    @Schema(description = "Savings coming from product sale pricing", example = "150.00")
    private BigDecimal saleSavings;

    @Schema(description = "Savings coming from discount code", example = "125.00")
    private BigDecimal codeSavings;

    @Schema(description = "Total savings (sale + code)", example = "275.00")
    private BigDecimal totalSavings;

    @Schema(description = "Final amount after all savings", example = "1125.00")
    private BigDecimal totalAfterDiscount;

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getSaleSavings() {
        return saleSavings;
    }

    public void setSaleSavings(BigDecimal saleSavings) {
        this.saleSavings = saleSavings;
    }

    public BigDecimal getCodeSavings() {
        return codeSavings;
    }

    public void setCodeSavings(BigDecimal codeSavings) {
        this.codeSavings = codeSavings;
    }

    public BigDecimal getTotalSavings() {
        return totalSavings;
    }

    public void setTotalSavings(BigDecimal totalSavings) {
        this.totalSavings = totalSavings;
    }

    public BigDecimal getTotalAfterDiscount() {
        return totalAfterDiscount;
    }

    public void setTotalAfterDiscount(BigDecimal totalAfterDiscount) {
        this.totalAfterDiscount = totalAfterDiscount;
    }
}
