package com.ecommerse.backend.services;

import com.ecommerse.backend.entities.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PricingService {

    public BigDecimal resolveEffectiveUnitPrice(Product product) {
        if (product == null || product.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        return roundCurrency(product.getEffectivePrice());
    }

    public BigDecimal resolveSaleSavingsPerUnit(Product product) {
        if (product == null || product.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal regularPrice = roundCurrency(product.getPrice());
        BigDecimal effectivePrice = resolveEffectiveUnitPrice(product);
        BigDecimal savings = regularPrice.subtract(effectivePrice);
        return savings.compareTo(BigDecimal.ZERO) > 0 ? savings : BigDecimal.ZERO;
    }

    public BigDecimal applyPercentageDiscount(BigDecimal amount, BigDecimal percentage) {
        if (amount == null || percentage == null) {
            return roundCurrency(amount != null ? amount : BigDecimal.ZERO);
        }
        if (percentage.compareTo(BigDecimal.ZERO) <= 0) {
            return roundCurrency(amount);
        }
        BigDecimal multiplier = BigDecimal.ONE.subtract(
                percentage.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        return roundCurrency(amount.multiply(multiplier));
    }

    public BigDecimal roundCurrency(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
