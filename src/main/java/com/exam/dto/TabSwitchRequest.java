package com.exam.dto;

public class TabSwitchRequest {
    private String reason;

    public TabSwitchRequest() {
    }

    public TabSwitchRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
