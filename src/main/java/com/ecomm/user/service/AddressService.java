package com.ecomm.user.service;

import com.ecomm.user.dto.request.AddressRequest;
import com.ecomm.user.dto.request.SetDefaultAddressRequest;
import com.ecomm.user.dto.response.AddressResponse;
import com.ecomm.user.entity.AddressType;
import com.ecomm.user.entity.UserAddress;
import com.ecomm.user.entity.UserProfile;
import com.ecomm.user.exception.ResourceNotFoundException;
import com.ecomm.user.exception.UserServiceException;
import com.ecomm.user.repository.UserAddressRepository;
import com.ecomm.user.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressService {

    public static final int MAX_ADDRESSES_PER_USER = 20;

    private final UserAddressRepository userAddressRepository;
    private final UserProfileRepository userProfileRepository;

    public AddressService(UserAddressRepository userAddressRepository,
                          UserProfileRepository userProfileRepository) {
        this.userAddressRepository = userAddressRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(String userUuid) {
        UserProfile profile = getProfileOrThrow(userUuid);
        return userAddressRepository
            .findByUserProfileIdOrderByDefaultShippingDescDefaultBillingDescCreatedAtDesc(profile.getId())
            .stream()
            .map(AddressService::mapToAddressResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AddressResponse getAddressByUuid(String userUuid, String addressUuid) {
        UserProfile profile = getProfileOrThrow(userUuid);
        UserAddress address = userAddressRepository.findByUuidAndUserProfileId(addressUuid, profile.getId())
            .orElseThrow(() -> new ResourceNotFoundException("UserAddress", "uuid", addressUuid));
        return mapToAddressResponse(address);
    }

    @Transactional
    public AddressResponse createAddress(String userUuid, AddressRequest request) {
        UserProfile profile = getProfileOrThrow(userUuid);
        long existingCount = userAddressRepository.countByUserProfileId(profile.getId());

        if (existingCount >= MAX_ADDRESSES_PER_USER) {
            throw new UserServiceException(
                "Maximum limit of " + MAX_ADDRESSES_PER_USER + " addresses per user reached",
                HttpStatus.BAD_REQUEST
            );
        }

        boolean makeDefaultShipping = request.isDefaultShipping() || existingCount == 0;
        boolean makeDefaultBilling = request.isDefaultBilling() || existingCount == 0;

        if (makeDefaultShipping) {
            userAddressRepository.clearDefaultShippingForUser(profile.getId());
        }
        if (makeDefaultBilling) {
            userAddressRepository.clearDefaultBillingForUser(profile.getId());
        }

        UserAddress address = new UserAddress();
        address.setUserProfile(profile);
        applyRequestToEntity(request, address);
        address.setDefaultShipping(makeDefaultShipping);
        address.setDefaultBilling(makeDefaultBilling);

        UserAddress saved = userAddressRepository.save(address);
        return mapToAddressResponse(saved);
    }

    @Transactional
    public AddressResponse updateAddress(String userUuid, String addressUuid, AddressRequest request) {
        UserProfile profile = getProfileOrThrow(userUuid);
        UserAddress address = userAddressRepository.findByUuidAndUserProfileId(addressUuid, profile.getId())
            .orElseThrow(() -> new ResourceNotFoundException("UserAddress", "uuid", addressUuid));

        if (request.isDefaultShipping() && !address.isDefaultShipping()) {
            userAddressRepository.clearDefaultShippingForUser(profile.getId());
            address.setDefaultShipping(true);
        }
        if (request.isDefaultBilling() && !address.isDefaultBilling()) {
            userAddressRepository.clearDefaultBillingForUser(profile.getId());
            address.setDefaultBilling(true);
        }

        applyRequestToEntity(request, address);
        UserAddress saved = userAddressRepository.save(address);
        return mapToAddressResponse(saved);
    }

    @Transactional
    public AddressResponse setDefaultAddress(String userUuid, String addressUuid, SetDefaultAddressRequest request) {
        UserProfile profile = getProfileOrThrow(userUuid);
        UserAddress address = userAddressRepository.findByUuidAndUserProfileId(addressUuid, profile.getId())
            .orElseThrow(() -> new ResourceNotFoundException("UserAddress", "uuid", addressUuid));

        boolean setShipping = request == null || request.getDefaultShipping() == null || Boolean.TRUE.equals(request.getDefaultShipping());
        boolean setBilling = request != null && Boolean.TRUE.equals(request.getDefaultBilling());

        if (setShipping) {
            userAddressRepository.clearDefaultShippingForUser(profile.getId());
            address.setDefaultShipping(true);
        }
        if (setBilling) {
            userAddressRepository.clearDefaultBillingForUser(profile.getId());
            address.setDefaultBilling(true);
        }

        UserAddress saved = userAddressRepository.save(address);
        return mapToAddressResponse(saved);
    }

    @Transactional
    public void deleteAddress(String userUuid, String addressUuid) {
        UserProfile profile = getProfileOrThrow(userUuid);
        UserAddress address = userAddressRepository.findByUuidAndUserProfileId(addressUuid, profile.getId())
            .orElseThrow(() -> new ResourceNotFoundException("UserAddress", "uuid", addressUuid));

        boolean wasDefaultShipping = address.isDefaultShipping();
        boolean wasDefaultBilling = address.isDefaultBilling();

        userAddressRepository.delete(address);
        userAddressRepository.flush();

        if (wasDefaultShipping || wasDefaultBilling) {
            List<UserAddress> remaining = userAddressRepository
                .findByUserProfileIdOrderByDefaultShippingDescDefaultBillingDescCreatedAtDesc(profile.getId());
            if (!remaining.isEmpty()) {
                UserAddress fallback = remaining.get(0);
                if (wasDefaultShipping) {
                    fallback.setDefaultShipping(true);
                }
                if (wasDefaultBilling) {
                    fallback.setDefaultBilling(true);
                }
                userAddressRepository.save(fallback);
            }
        }
    }

    private UserProfile getProfileOrThrow(String userUuid) {
        return userProfileRepository.findByUuid(userUuid)
            .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "uuid", userUuid));
    }

    private void applyRequestToEntity(AddressRequest request, UserAddress address) {
        address.setLabel(request.getLabel());
        address.setRecipientName(request.getRecipientName().trim());
        address.setPhoneNumber(request.getPhoneNumber().trim());
        address.setAddressType(request.getAddressType() != null ? request.getAddressType() : AddressType.BOTH);
        address.setStreetLine1(request.getStreetLine1().trim());
        address.setStreetLine2(request.getStreetLine2() != null ? request.getStreetLine2().trim() : null);
        address.setCity(request.getCity().trim());
        address.setStateProvince(request.getStateProvince().trim());
        address.setPostalCode(request.getPostalCode().trim());
        address.setCountryCode(request.getCountryCode() != null ? request.getCountryCode().trim().toUpperCase() : "US");
        address.setDeliveryInstructions(request.getDeliveryInstructions());
    }

    public static AddressResponse mapToAddressResponse(UserAddress address) {
        AddressResponse response = new AddressResponse();
        response.setUuid(address.getUuid());
        response.setLabel(address.getLabel());
        response.setRecipientName(address.getRecipientName());
        response.setPhoneNumber(address.getPhoneNumber());
        response.setAddressType(address.getAddressType());
        response.setStreetLine1(address.getStreetLine1());
        response.setStreetLine2(address.getStreetLine2());
        response.setCity(address.getCity());
        response.setStateProvince(address.getStateProvince());
        response.setPostalCode(address.getPostalCode());
        response.setCountryCode(address.getCountryCode());
        response.setDefaultShipping(address.isDefaultShipping());
        response.setDefaultBilling(address.isDefaultBilling());
        response.setDeliveryInstructions(address.getDeliveryInstructions());
        response.setCreatedAt(address.getCreatedAt());
        response.setUpdatedAt(address.getUpdatedAt());
        return response;
    }
}

