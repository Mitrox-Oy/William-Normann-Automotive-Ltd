package com.ecommerse.backend.enums;

public enum OemType {
    OEM,
    AFTERMARKET;

    public String toApiValue() {
        return name().toLowerCase();
    }
}
