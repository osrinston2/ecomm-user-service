package com.ecomm.user.controller;

import com.ecomm.user.dto.request.UpdateProfileRequest;
import com.ecomm.user.dto.response.ApiResponse;
import com.ecomm.user.dto.response.UserProfileResponse;
import com.ecomm.user.security.UserPrincipal;
import com.ecomm.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "1. User Profile", description = "Persona-aware user profile endpoints (Customer, Seller, Admin)")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns general profile fields plus shipping/billing addresses for Customers and seller storefront details for Sellers.")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUserProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponse profile = userProfileService.getProfileByUuid(principal.getUuid());
        return ResponseEntity.ok(ApiResponse.ok("User profile retrieved successfully", profile));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Updates general profile fields (name, phone, avatar, DOB, gender, bio, preferred language/currency, marketing opt-in).")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateCurrentUserProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse updated = userProfileService.updateProfile(principal.getUuid(), request);
        return ResponseEntity.ok(ApiResponse.ok("User profile updated successfully", updated));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT') or #principal.uuid == #uuid")
    @Operation(summary = "Get user profile by UUID", description = "Accessible by ADMIN, SUPPORT, or the user themselves.")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfileByUuid(
            @PathVariable String uuid,
            @AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponse profile = userProfileService.getProfileByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.ok("User details retrieved successfully", profile));
    }
}
