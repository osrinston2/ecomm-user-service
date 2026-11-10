package com.ecomm.user.controller;

import com.ecomm.user.dto.request.AddressRequest;
import com.ecomm.user.dto.request.SetDefaultAddressRequest;
import com.ecomm.user.dto.response.AddressResponse;
import com.ecomm.user.dto.response.ApiResponse;
import com.ecomm.user.security.UserPrincipal;
import com.ecomm.user.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@Tag(name = "2. Customer Address Book", description = "Manage customer shipping and billing addresses")
public class UserAddressController {

    private final AddressService addressService;

    public UserAddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    @Operation(summary = "List my addresses", description = "Returns all saved shipping and billing addresses ordered with defaults first.")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<AddressResponse> addresses = addressService.getUserAddresses(principal.getUuid());
        return ResponseEntity.ok(ApiResponse.ok("Addresses retrieved successfully", addresses));
    }

    @GetMapping("/{addressUuid}")
    @Operation(summary = "Get address by UUID", description = "Returns details for a specific address owned by the current user.")
    public ResponseEntity<ApiResponse<AddressResponse>> getMyAddressByUuid(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String addressUuid) {
        AddressResponse address = addressService.getAddressByUuid(principal.getUuid(), addressUuid);
        return ResponseEntity.ok(ApiResponse.ok("Address retrieved successfully", address));
    }

    @PostMapping
    @Operation(summary = "Create a new address", description = "Adds a new shipping/billing address (up to 20 per user). First address is automatically marked default.")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse created = addressService.createAddress(principal.getUuid(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Address created successfully", created));
    }

    @PutMapping("/{addressUuid}")
    @Operation(summary = "Update an existing address", description = "Updates address fields and default shipping/billing flags.")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String addressUuid,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse updated = addressService.updateAddress(principal.getUuid(), addressUuid, request);
        return ResponseEntity.ok(ApiResponse.ok("Address updated successfully", updated));
    }

    @PatchMapping("/{addressUuid}/default")
    @Operation(summary = "Set default shipping/billing address", description = "Marks the specified address as default shipping and/or default billing.")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String addressUuid,
            @RequestBody(required = false) SetDefaultAddressRequest request) {
        AddressResponse updated = addressService.setDefaultAddress(principal.getUuid(), addressUuid, request);
        return ResponseEntity.ok(ApiResponse.ok("Default address preferences updated", updated));
    }

    @DeleteMapping("/{addressUuid}")
    @Operation(summary = "Delete an address", description = "Deletes the specified address and promotes a fallback default if needed.")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String addressUuid) {
        addressService.deleteAddress(principal.getUuid(), addressUuid);
        return ResponseEntity.ok(ApiResponse.ok("Address deleted successfully"));
    }
}
