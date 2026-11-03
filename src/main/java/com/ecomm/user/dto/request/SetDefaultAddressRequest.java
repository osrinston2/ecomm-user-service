package com.ecomm.user.dto.request;

public class SetDefaultAddressRequest {

    private Boolean defaultShipping;
    private Boolean defaultBilling;

    public SetDefaultAddressRequest() {}

    public Boolean getDefaultShipping() {
        return defaultShipping;
    }

    public void setDefaultShipping(Boolean defaultShipping) {
        this.defaultShipping = defaultShipping;
    }

    public Boolean getDefaultBilling() {
        return defaultBilling;
    }

    public void setDefaultBilling(Boolean defaultBilling) {
        this.defaultBilling = defaultBilling;
    }
}

