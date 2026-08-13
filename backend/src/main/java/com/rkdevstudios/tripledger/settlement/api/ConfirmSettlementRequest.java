package com.rkdevstudios.tripledger.settlement.api;

import jakarta.validation.constraints.NotBlank;

public class ConfirmSettlementRequest {

    @NotBlank
    private String sessionId;

    public ConfirmSettlementRequest() {
    }

    public ConfirmSettlementRequest(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
