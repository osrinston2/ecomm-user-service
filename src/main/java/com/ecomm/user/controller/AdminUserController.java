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
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
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
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserStatus(
            @PathVariable String uuid,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        UserProfileResponse updated = userProfileService.updateUserStatus(uuid, request);
        return ResponseEntity.ok(ApiResponse.ok("User status updated successfully", updated));
    }

    @PatchMapping("/sellers/{sellerUuid}/verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SellerProfileResponse>> updateSellerVerification(
            @PathVariable String sellerUuid,
            @Valid @RequestBody UpdateSellerVerificationRequest request) {
        SellerProfileResponse updated = sellerProfileService.updateSellerVerification(sellerUuid, request);
        return ResponseEntity.ok(ApiResponse.ok("Seller verification status updated successfully", updated));
    }
}

