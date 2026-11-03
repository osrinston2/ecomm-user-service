package com.ecomm.user.dto.request;

import com.ecomm.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateUserStatusRequest {

    @NotNull(message = "User status is required")
    private UserStatus status;

    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;

    public UpdateUserStatusRequest() {}

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

