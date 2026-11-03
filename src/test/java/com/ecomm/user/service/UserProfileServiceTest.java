package com.ecomm.user.service;

import com.ecomm.user.dto.request.UpdateProfileRequest;
import com.ecomm.user.dto.response.UserProfileResponse;
import com.ecomm.user.entity.*;
import com.ecomm.user.repository.SellerProfileRepository;
import com.ecomm.user.repository.UserAddressRepository;
import com.ecomm.user.repository.UserProfileRepository;
import com.ecomm.user.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private SellerProfileRepository sellerProfileRepository;

    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService(
            userProfileRepository,
            userAddressRepository,
            sellerProfileRepository
        );
    }

    @Test
    void getProfileByUuid_CustomerPersona_IncludesAddressesAndOmitsSellerProfile() {
        UserProfile customer = new UserProfile();
        customer.setId(1L);
        customer.setUuid("cust-uuid-1");
        customer.setEmail("customer@ecomm.com");
        customer.setFirstName("Alice");
        customer.setLastName("Customer");
        customer.setRoles(Set.of(RoleName.ROLE_CUSTOMER));

        UserAddress address = new UserAddress();
        address.setUuid("addr-1");
        address.setLabel("Home");
        address.setRecipientName("Alice Customer");
        address.setStreetLine1("742 Evergreen Terrace");
        address.setCity("Springfield");
        address.setStateProvince("IL");
        address.setPostalCode("62704");
        address.setCountryCode("US");
        address.setDefaultShipping(true);
        address.setDefaultBilling(true);

        when(userProfileRepository.findByUuid("cust-uuid-1")).thenReturn(Optional.of(customer));
        when(userAddressRepository.findByUserProfileIdOrderByDefaultShippingDescDefaultBillingDescCreatedAtDesc(1L))
            .thenReturn(List.of(address));
        when(sellerProfileRepository.findByUserProfileId(1L)).thenReturn(Optional.empty());

        UserProfileResponse response = userProfileService.getProfileByUuid("cust-uuid-1");

        assertEquals("customer@ecomm.com", response.getEmail());
        assertNotNull(response.getAddresses(), "Customer profile should include shipping/billing address book");
        assertEquals(1, response.getAddresses().size());
        assertEquals("742 Evergreen Terrace", response.getAddresses().get(0).getStreetLine1());
        assertNull(response.getSellerProfile(), "Pure customer profile should not include sellerProfile extension");
    }

    @Test
    void getProfileByUuid_SellerPersona_IncludesSellerProfileExtension() {
        UserProfile seller = new UserProfile();
        seller.setId(3L);
        seller.setUuid("seller-uuid-3");
        seller.setEmail("seller@ecomm.com");
        seller.setFirstName("Bob");
        seller.setLastName("Merchant");
        seller.setRoles(Set.of(RoleName.ROLE_SELLER));

        SellerProfile storefront = new SellerProfile();
        storefront.setUuid("store-uuid-1");
        storefront.setUserProfile(seller);
        storefront.setStoreName("Bob Tech & Gadgets");
        storefront.setStoreSlug("bob-tech-gadgets");
        storefront.setVerificationStatus(SellerVerificationStatus.VERIFIED);

        when(userProfileRepository.findByUuid("seller-uuid-3")).thenReturn(Optional.of(seller));
        when(userAddressRepository.findByUserProfileIdOrderByDefaultShippingDescDefaultBillingDescCreatedAtDesc(3L))
            .thenReturn(List.of());
        when(sellerProfileRepository.findByUserProfileId(3L)).thenReturn(Optional.of(storefront));

        UserProfileResponse response = userProfileService.getProfileByUuid("seller-uuid-3");

        assertEquals("seller@ecomm.com", response.getEmail());
        assertNotNull(response.getSellerProfile(), "Seller profile should include merchant storefront extension");
        assertEquals("Bob Tech & Gadgets", response.getSellerProfile().getStoreName());
        assertEquals(SellerVerificationStatus.VERIFIED, response.getSellerProfile().getVerificationStatus());
        assertNull(response.getAddresses(), "Pure seller with no addresses should omit customer address book");
    }

    @Test
    void resolveAndSyncPrincipal_NewUserFromAuthService_LazyProvisionsProfile() {
        when(userProfileRepository.findByUuid("new-uuid-99")).thenReturn(Optional.empty());
        when(userProfileRepository.findByEmailIgnoreCase("newbuyer@ecomm.com")).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserPrincipal principal = userProfileService.resolveAndSyncPrincipal(
            "new-uuid-99",
            "newbuyer@ecomm.com",
            List.of("ROLE_CUSTOMER")
        );

        assertEquals("new-uuid-99", principal.getUuid());
        assertEquals("newbuyer@ecomm.com", principal.getEmail());
        assertTrue(principal.hasRole(RoleName.ROLE_CUSTOMER));
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void updateProfile_ValidFields_UpdatesGeneralInformation() {
        UserProfile existing = new UserProfile();
        existing.setId(1L);
        existing.setUuid("cust-uuid-1");
        existing.setEmail("customer@ecomm.com");
        existing.setFirstName("Alice");
        existing.setLastName("Customer");
        existing.setRoles(Set.of(RoleName.ROLE_CUSTOMER));

        when(userProfileRepository.findByUuid("cust-uuid-1")).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userAddressRepository.findByUserProfileIdOrderByDefaultShippingDescDefaultBillingDescCreatedAtDesc(1L))
            .thenReturn(List.of());
        when(sellerProfileRepository.findByUserProfileId(1L)).thenReturn(Optional.empty());

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Alicia");
        request.setPhoneNumber("+1999888777");
        request.setPreferredCurrency("MYR");
        request.setBio("Updated profile bio");

        UserProfileResponse updated = userProfileService.updateProfile("cust-uuid-1", request);

        assertEquals("Alicia", updated.getFirstName());
        assertEquals("+1999888777", updated.getPhoneNumber());
        assertEquals("MYR", updated.getPreferredCurrency());
        assertEquals("Updated profile bio", updated.getBio());
    }
}

