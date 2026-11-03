package com.ecomm.user.dto.request;

import com.ecomm.user.entity.SellerVerificationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateSellerVerificationRequest {

    @NotNull(message = "Verification status is required")
    private SellerVerificationStatus verificationStatus;

    @Size(max = 500, message = "Review notes must not exceed 500 characters")
    private String reviewNotes;

    public UpdateSellerVerificationRequest() {}

    public SellerVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(SellerVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }
}

