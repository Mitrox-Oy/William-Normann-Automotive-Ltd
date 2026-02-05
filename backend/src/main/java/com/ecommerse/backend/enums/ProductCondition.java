package com.ecommerse.backend.enums;

public enum ProductCondition {
    NEW,
    USED,
    REFURBISHED;

    public static ProductCondition fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ProductCondition condition : values()) {
            if (condition.name().equalsIgnoreCase(value)) {
                return condition;
            }
        }
        return null;
    }

    public String toApiValue() {
        return name().toLowerCase();
    }
}
