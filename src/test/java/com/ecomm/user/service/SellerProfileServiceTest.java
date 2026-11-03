package com.ecomm.user.service;

import com.ecomm.user.dto.request.SellerProfileRequest;
import com.ecomm.user.dto.response.SellerProfileResponse;
import com.ecomm.user.entity.SellerProfile;
import com.ecomm.user.entity.SellerVerificationStatus;
import com.ecomm.user.entity.UserProfile;
import com.ecomm.user.exception.UserServiceException;
import com.ecomm.user.repository.SellerProfileRepository;
import com.ecomm.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerProfileServiceTest {

    @Mock
    private SellerProfileRepository sellerProfileRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    private SellerProfileService sellerProfileService;

    @BeforeEach
    void setUp() {
        sellerProfileService = new SellerProfileService(sellerProfileRepository, userProfileRepository);
    }

    @Test
    void upsertSellerProfile_ValidMerchantDetails_CreatesStorefrontWithGeneratedSlug() {
        UserProfile seller = new UserProfile();
        seller.setId(3L);
        seller.setUuid("seller-uuid-3");
        seller.setEmail("seller@ecomm.com");

        when(userProfileRepository.findByUuid("seller-uuid-3")).thenReturn(Optional.of(seller));
        when(sellerProfileRepository.findByUserProfileId(3L)).thenReturn(Optional.empty());
        when(sellerProfileRepository.existsByStoreSlugIgnoreCase("nova-electronics")).thenReturn(false);
        when(sellerProfileRepository.save(any(SellerProfile.class))).thenAnswer(inv -> {
            SellerProfile s = inv.getArgument(0);
            s.setUuid("seller-prof-1");
            return s;
        });

        SellerProfileRequest request = new SellerProfileRequest();
        request.setStoreName("Nova Electronics");
        request.setBusinessName("Nova Electronics Inc.");
        request.setTaxId("EIN-99-1122334");

        SellerProfileResponse response = sellerProfileService.upsertSellerProfile("seller-uuid-3", request);

        assertEquals("Nova Electronics", response.getStoreName());
        assertEquals("nova-electronics", response.getStoreSlug());
        assertEquals("EIN-99-1122334", response.getTaxId());
        assertEquals(SellerVerificationStatus.PENDING, response.getVerificationStatus());
    }

    @Test
    void upsertSellerProfile_DuplicateSlug_ThrowsConflictException() {
        UserProfile seller = new UserProfile();
        seller.setId(3L);
        seller.setUuid("seller-uuid-3");

        when(userProfileRepository.findByUuid("seller-uuid-3")).thenReturn(Optional.of(seller));
        when(sellerProfileRepository.findByUserProfileId(3L)).thenReturn(Optional.empty());
        when(sellerProfileRepository.existsByStoreSlugIgnoreCase("bob-tech-gadgets")).thenReturn(true);

        SellerProfileRequest request = new SellerProfileRequest();
        request.setStoreName("Bob Tech & Gadgets");
        request.setStoreSlug("bob-tech-gadgets");

        assertThrows(UserServiceException.class,
            () -> sellerProfileService.upsertSellerProfile("seller-uuid-3", request));
    }
}

