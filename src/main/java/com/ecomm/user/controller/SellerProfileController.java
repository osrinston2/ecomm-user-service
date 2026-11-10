package com.ecomm.user.controller;

import com.ecomm.user.dto.request.SellerProfileRequest;
import com.ecomm.user.dto.response.ApiResponse;
import com.ecomm.user.dto.response.SellerProfileResponse;
import com.ecomm.user.security.UserPrincipal;
import com.ecomm.user.service.SellerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "3. Seller & Merchant Storefront", description = "Merchant profile management for ROLE_SELLER and public storefront lookup")
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;

    public SellerProfileController(SellerProfileService sellerProfileService) {
        this.sellerProfileService = sellerProfileService;
    }

    @GetMapping("/api/v1/users/me/seller-profile")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "Get my seller profile", description = "Retrieves full merchant storefront and business registration details for the authenticated seller.")
    public ResponseEntity<ApiResponse<SellerProfileResponse>> getMySellerProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        SellerProfileResponse sellerProfile = sellerProfileService.getSellerProfileByUserUuid(principal.getUuid());
        return ResponseEntity.ok(ApiResponse.ok("Seller profile retrieved successfully", sellerProfile));
    }

    @PutMapping("/api/v1/users/me/seller-profile")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "Create or update my seller profile", description = "Upserts the authenticated seller's merchant storefront, business registration, tax ID, and return/shipping policies.")
    public ResponseEntity<ApiResponse<SellerProfileResponse>> upsertMySellerProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SellerProfileRequest request) {
        SellerProfileResponse updated = sellerProfileService.upsertSellerProfile(principal.getUuid(), request);
        return ResponseEntity.ok(ApiResponse.ok("Seller profile saved successfully", updated));
    }

    @GetMapping("/api/v1/sellers/{slugOrUuid}")
    @SecurityRequirements
    @Operation(summary = "Get public merchant storefront (Public)", description = "Publicly accessible endpoint to view a seller's storefront by storeSlug (e.g. bob-tech-gadgets) or UUID. Redacts private tax and registration numbers.")
    public ResponseEntity<ApiResponse<SellerProfileResponse>> getPublicSellerStorefront(
            @PathVariable String slugOrUuid) {
        SellerProfileResponse storefront = sellerProfileService.getPublicSellerStorefront(slugOrUuid);
        return ResponseEntity.ok(ApiResponse.ok("Merchant storefront retrieved successfully", storefront));
    }
}
