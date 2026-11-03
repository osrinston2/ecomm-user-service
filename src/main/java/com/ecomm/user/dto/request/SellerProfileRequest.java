package com.ecomm.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SellerProfileRequest {

    @NotBlank(message = "Store name is required")
    @Size(min = 2, max = 150, message = "Store name must be between 2 and 150 characters")
    private String storeName;

    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Store slug must be lowercase alphanumeric with hyphens (e.g. bob-tech-gadgets)")
    @Size(min = 2, max = 150, message = "Store slug must be between 2 and 150 characters")
    private String storeSlug;

    @Size(max = 2000, message = "Store description must not exceed 2000 characters")
    private String storeDescription;

    @Size(max = 500, message = "Store logo URL must not exceed 500 characters")
    private String storeLogoUrl;

    @Size(max = 500, message = "Store banner URL must not exceed 500 characters")
    private String storeBannerUrl;

    @Size(max = 200, message = "Business name must not exceed 200 characters")
    private String businessName;

    @Size(max = 100, message = "Business registration number must not exceed 100 characters")
    private String businessRegistrationNumber;

    @Size(max = 100, message = "Tax ID must not exceed 100 characters")
    private String taxId;

    @Email(message = "Support email must be a valid email address")
    @Size(max = 150, message = "Support email must not exceed 150 characters")
    private String supportEmail;

    @Size(max = 30, message = "Support phone must not exceed 30 characters")
    private String supportPhone;

    @Size(max = 2000, message = "Return policy must not exceed 2000 characters")
    private String returnPolicy;

    @Size(max = 2000, message = "Shipping policy must not exceed 2000 characters")
    private String shippingPolicy;

    public SellerProfileRequest() {}

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStoreSlug() {
        return storeSlug;
    }

    public void setStoreSlug(String storeSlug) {
        this.storeSlug = storeSlug;
    }

    public String getStoreDescription() {
        return storeDescription;
    }

    public void setStoreDescription(String storeDescription) {
        this.storeDescription = storeDescription;
    }

    public String getStoreLogoUrl() {
        return storeLogoUrl;
    }

    public void setStoreLogoUrl(String storeLogoUrl) {
        this.storeLogoUrl = storeLogoUrl;
    }

    public String getStoreBannerUrl() {
        return storeBannerUrl;
    }

    public void setStoreBannerUrl(String storeBannerUrl) {
        this.storeBannerUrl = storeBannerUrl;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getBusinessRegistrationNumber() {
        return businessRegistrationNumber;
    }

    public void setBusinessRegistrationNumber(String businessRegistrationNumber) {
        this.businessRegistrationNumber = businessRegistrationNumber;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public String getSupportPhone() {
        return supportPhone;
    }

    public void setSupportPhone(String supportPhone) {
        this.supportPhone = supportPhone;
    }

    public String getReturnPolicy() {
        return returnPolicy;
    }

    public void setReturnPolicy(String returnPolicy) {
        this.returnPolicy = returnPolicy;
    }

    public String getShippingPolicy() {
        return shippingPolicy;
    }

    public void setShippingPolicy(String shippingPolicy) {
        this.shippingPolicy = shippingPolicy;
    }
}

