package com.ecommerse.backend.enums;

public enum CompatibilityMode {
    UNIVERSAL,
    VEHICLE_SPECIFIC;

    public String toApiValue() {
        return name().toLowerCase();
    }
}
