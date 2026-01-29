package com.ecommerse.backend.dto;

public class FinalizeCheckoutRequest {

    private String sessionId;

    public FinalizeCheckoutRequest() {
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}

