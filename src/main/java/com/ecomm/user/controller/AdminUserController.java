package com.ecomm.user.controller;

import com.ecomm.user.dto.request.UpdateSellerVerificationRequest;
import com.ecomm.user.dto.request.UpdateUserStatusRequest;
import com.ecomm.user.dto.response.ApiResponse;
import com.ecomm.user.dto.response.PagedResponse;
import com.ecomm.user.dto.response.SellerProfileResponse;
import com.ecomm.user.dto.response.UserProfileResponse;
import com.ecomm.user.dto.response.UserSummaryResponse;
import com.ecomm.user.entity.RoleName;
import com.ecomm.user.entity.UserStatus;
import com.ecomm.user.service.SellerProfileService;
import com.ecomm.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "4. Admin & Support Management", description = "Platform user search, account status control, and merchant verification")
public class AdminUserController {

    private final UserProfileService userProfileService;
    private final SellerProfileService sellerProfileService;

    public AdminUserController(UserProfileService userProfileService,
                               SellerProfileService sellerProfileService) {
        this.userProfileService = userProfileService;
        this.sellerProfileService = sellerProfileService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
    @Operation(summary = "Search and filter users (Paginated)", description = "Filter users by status, role, and keyword search across email, first name, and last name.")
    public ResponseEntity<ApiResponse<PagedResponse<UserSummaryResponse>>> searchUsers(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)),
            Sort.by(Sort.Direction.DESC, "createdAt"));
        PagedResponse<UserSummaryResponse> result = userProfileService.searchUsers(status, role, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved successfully", result));
    }

    @PatchMapping("/users/{uuid}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user account status", description = "Activate, suspend, or mark a user account as deleted (ROLE_ADMIN only).")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserStatus(
            @PathVariable String uuid,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        UserProfileResponse updated = userProfileService.updateUserStatus(uuid, request);
        return ResponseEntity.ok(ApiResponse.ok("User status updated successfully", updated));
    }

    @PatchMapping("/sellers/{sellerUuid}/verification")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Verify or reject a seller storefront", description = "Approve (VERIFIED), reject (REJECTED), or suspend (SUSPENDED) a merchant storefront (ROLE_ADMIN only).")
    public ResponseEntity<ApiResponse<SellerProfileResponse>> updateSellerVerification(
            @PathVariable String sellerUuid,
            @Valid @RequestBody UpdateSellerVerificationRequest request) {
        SellerProfileResponse updated = sellerProfileService.updateSellerVerification(sellerUuid, request);
        return ResponseEntity.ok(ApiResponse.ok("Seller verification status updated successfully", updated));
    }
}
