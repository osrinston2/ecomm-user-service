package com.ecomm.user.service;

import com.ecomm.user.dto.request.SellerProfileRequest;
import com.ecomm.user.dto.request.UpdateSellerVerificationRequest;
import com.ecomm.user.dto.response.SellerProfileResponse;
import com.ecomm.user.entity.SellerProfile;
import com.ecomm.user.entity.SellerVerificationStatus;
import com.ecomm.user.entity.UserProfile;
import com.ecomm.user.exception.ResourceNotFoundException;
import com.ecomm.user.exception.UserServiceException;
import com.ecomm.user.repository.SellerProfileRepository;
import com.ecomm.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class SellerProfileService {

    private static final Logger log = LoggerFactory.getLogger(SellerProfileService.class);

    private final SellerProfileRepository sellerProfileRepository;
    private final UserProfileRepository userProfileRepository;

    public SellerProfileService(SellerProfileRepository sellerProfileRepository,
                                UserProfileRepository userProfileRepository) {
        this.sellerProfileRepository = sellerProfileRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public SellerProfileResponse getSellerProfileByUserUuid(String userUuid) {
        UserProfile userProfile = userProfileRepository.findByUuid(userUuid)
            .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "uuid", userUuid));

        SellerProfile sellerProfile = sellerProfileRepository.findByUserProfileId(userProfile.getId())
            .orElseThrow(() -> new ResourceNotFoundException("SellerProfile", "userUuid", userUuid));

        return mapToSellerResponse(sellerProfile, true);
    }

    @Transactional
    public SellerProfileResponse upsertSellerProfile(String userUuid, SellerProfileRequest request) {
        UserProfile userProfile = userProfileRepository.findByUuid(userUuid)
            .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "uuid", userUuid));

        SellerProfile sellerProfile = sellerProfileRepository.findByUserProfileId(userProfile.getId())
            .orElseGet(() -> {
                SellerProfile created = new SellerProfile();
                created.setUserProfile(userProfile);
                created.setVerificationStatus(SellerVerificationStatus.PENDING);
                return created;
            });

        String resolvedSlug = (request.getStoreSlug() != null && !request.getStoreSlug().isBlank())
            ? request.getStoreSlug().trim().toLowerCase(Locale.ROOT)
            : generateSlug(request.getStoreName());

        boolean slugTaken = sellerProfile.getId() == null
            ? sellerProfileRepository.existsByStoreSlugIgnoreCase(resolvedSlug)
            : sellerProfileRepository.existsByStoreSlugIgnoreCaseAndIdNot(resolvedSlug, sellerProfile.getId());

        if (slugTaken) {
            throw new UserServiceException("Store slug '" + resolvedSlug + "' is already in use", HttpStatus.CONFLICT);
        }

        sellerProfile.setStoreName(request.getStoreName().trim());
        sellerProfile.setStoreSlug(resolvedSlug);
        sellerProfile.setStoreDescription(request.getStoreDescription());
        sellerProfile.setStoreLogoUrl(request.getStoreLogoUrl());
        sellerProfile.setStoreBannerUrl(request.getStoreBannerUrl());
        sellerProfile.setBusinessName(request.getBusinessName());
        sellerProfile.setBusinessRegistrationNumber(request.getBusinessRegistrationNumber());
        sellerProfile.setTaxId(request.getTaxId());
        sellerProfile.setSupportEmail(request.getSupportEmail() != null ? request.getSupportEmail() : userProfile.getEmail());
        sellerProfile.setSupportPhone(request.getSupportPhone() != null ? request.getSupportPhone() : userProfile.getPhoneNumber());
        sellerProfile.setReturnPolicy(request.getReturnPolicy());
        sellerProfile.setShippingPolicy(request.getShippingPolicy());

        SellerProfile saved = sellerProfileRepository.save(sellerProfile);
        log.info("Upserted SellerProfile '{}' (slug: {}) for user {}", saved.getStoreName(), saved.getStoreSlug(), userUuid);
        return mapToSellerResponse(saved, true);
    }

    @Transactional(readOnly = true)
    public SellerProfileResponse getPublicSellerStorefront(String slugOrUuid) {
        SellerProfile sellerProfile = sellerProfileRepository.findByStoreSlugIgnoreCase(slugOrUuid)
            .or(() -> sellerProfileRepository.findByUuid(slugOrUuid))
            .orElseThrow(() -> new ResourceNotFoundException("SellerStorefront", "slugOrUuid", slugOrUuid));

        return mapToSellerResponse(sellerProfile, false);
    }

    @Transactional
    public SellerProfileResponse updateSellerVerification(String sellerUuid, UpdateSellerVerificationRequest request) {
        SellerProfile sellerProfile = sellerProfileRepository.findByUuid(sellerUuid)
            .or(() -> userProfileRepository.findByUuid(sellerUuid)
                .flatMap(u -> sellerProfileRepository.findByUserProfileId(u.getId())))
            .orElseThrow(() -> new ResourceNotFoundException("SellerProfile", "uuid", sellerUuid));

        sellerProfile.setVerificationStatus(request.getVerificationStatus());
        if (request.getVerificationStatus() == SellerVerificationStatus.VERIFIED) {
            sellerProfile.setVerifiedAt(Instant.now());
        }

        SellerProfile saved = sellerProfileRepository.save(sellerProfile);
        log.info("Updated seller verification status for '{}' to {} (notes: {})",
            saved.getStoreName(), request.getVerificationStatus(), request.getReviewNotes());
        return mapToSellerResponse(saved, true);
    }

    private String generateSlug(String storeName) {
        String slug = storeName.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "store-" + System.currentTimeMillis() : slug;
    }

    public static SellerProfileResponse mapToSellerResponse(SellerProfile seller, boolean includePrivateBusinessFields) {
        SellerProfileResponse response = new SellerProfileResponse();
        response.setUuid(seller.getUuid());
        if (seller.getUserProfile() != null) {
            response.setOwnerUserUuid(seller.getUserProfile().getUuid());
        }
        response.setStoreName(seller.getStoreName());
        response.setStoreSlug(seller.getStoreSlug());
        response.setStoreDescription(seller.getStoreDescription());
        response.setStoreLogoUrl(seller.getStoreLogoUrl());
        response.setStoreBannerUrl(seller.getStoreBannerUrl());
        response.setBusinessName(seller.getBusinessName());
        if (includePrivateBusinessFields) {
            response.setBusinessRegistrationNumber(seller.getBusinessRegistrationNumber());
            response.setTaxId(seller.getTaxId());
        }
        response.setSupportEmail(seller.getSupportEmail());
        response.setSupportPhone(seller.getSupportPhone());
        response.setReturnPolicy(seller.getReturnPolicy());
        response.setShippingPolicy(seller.getShippingPolicy());
        response.setVerificationStatus(seller.getVerificationStatus());
        response.setVerifiedAt(seller.getVerifiedAt());
        response.setCreatedAt(seller.getCreatedAt());
        response.setUpdatedAt(seller.getUpdatedAt());
        return response;
    }
}

