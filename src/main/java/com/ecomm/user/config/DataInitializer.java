package com.ecomm.user.config;

import com.ecomm.user.entity.*;
import com.ecomm.user.repository.SellerProfileRepository;
import com.ecomm.user.repository.UserAddressRepository;
import com.ecomm.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner seedDefaultUserProfiles(UserProfileRepository userProfileRepository,
                                                     UserAddressRepository userAddressRepository,
                                                     SellerProfileRepository sellerProfileRepository) {
        return args -> {
            // 1. Seed Customer Profile (customer@ecomm.com)
            if (!userProfileRepository.existsByEmailIgnoreCase("customer@ecomm.com")) {
                UserProfile customer = new UserProfile();
                customer.setUuid("c0000000-0000-0000-0000-000000000001");
                customer.setEmail("customer@ecomm.com");
                customer.setFirstName("Alice");
                customer.setLastName("Customer");
                customer.setPhoneNumber("+1000000001");
                customer.setAvatarUrl("https://cdn.ecomm.com/avatars/alice.png");
                customer.setDateOfBirth(LocalDate.of(1994, 6, 15));
                customer.setGender("FEMALE");
                customer.setBio("Avid online shopper and tech enthusiast.");
                customer.setPreferredLanguage("en");
                customer.setPreferredCurrency("USD");
                customer.setMarketingOptIn(true);
                customer.setStatus(UserStatus.ACTIVE);
                customer.setRoles(Set.of(RoleName.ROLE_CUSTOMER));
                customer = userProfileRepository.save(customer);

                UserAddress homeAddress = new UserAddress();
                homeAddress.setUuid("addr-0000-0000-0000-000000000001");
                homeAddress.setUserProfile(customer);
                homeAddress.setLabel("Home");
                homeAddress.setRecipientName("Alice Customer");
                homeAddress.setPhoneNumber("+1000000001");
                homeAddress.setAddressType(AddressType.BOTH);
                homeAddress.setStreetLine1("742 Evergreen Terrace");
                homeAddress.setStreetLine2("Apt 2B");
                homeAddress.setCity("Springfield");
                homeAddress.setStateProvince("IL");
                homeAddress.setPostalCode("62704");
                homeAddress.setCountryCode("US");
                homeAddress.setDefaultShipping(true);
                homeAddress.setDefaultBilling(true);
                homeAddress.setDeliveryInstructions("Leave package at the front porch near the planter.");
                userAddressRepository.save(homeAddress);

                log.info("Initialized default customer profile and address book for customer@ecomm.com");
            }

            // 2. Seed Admin Profile (admin@ecomm.com)
            if (!userProfileRepository.existsByEmailIgnoreCase("admin@ecomm.com")) {
                UserProfile admin = new UserProfile();
                admin.setUuid("a0000000-0000-0000-0000-000000000002");
                admin.setEmail("admin@ecomm.com");
                admin.setFirstName("Admin");
                admin.setLastName("Root");
                admin.setPhoneNumber("+1000000002");
                admin.setAvatarUrl("https://cdn.ecomm.com/avatars/admin.png");
                admin.setDateOfBirth(LocalDate.of(1988, 1, 10));
                admin.setGender("PREFER_NOT_TO_SAY");
                admin.setBio("Platform Administrator.");
                admin.setPreferredLanguage("en");
                admin.setPreferredCurrency("USD");
                admin.setMarketingOptIn(false);
                admin.setStatus(UserStatus.ACTIVE);
                admin.setRoles(Set.of(RoleName.ROLE_ADMIN, RoleName.ROLE_CUSTOMER));
                userProfileRepository.save(admin);

                log.info("Initialized default admin profile for admin@ecomm.com");
            }

            // 3. Seed Seller / Merchant Profile (seller@ecomm.com)
            if (!userProfileRepository.existsByEmailIgnoreCase("seller@ecomm.com")) {
                UserProfile seller = new UserProfile();
                seller.setUuid("s0000000-0000-0000-0000-000000000003");
                seller.setEmail("seller@ecomm.com");
                seller.setFirstName("Bob");
                seller.setLastName("Merchant");
                seller.setPhoneNumber("+1000000003");
                seller.setAvatarUrl("https://cdn.ecomm.com/avatars/bob.png");
                seller.setDateOfBirth(LocalDate.of(1985, 11, 22));
                seller.setGender("MALE");
                seller.setBio("Founder of Bob Tech & Gadgets Official Store.");
                seller.setPreferredLanguage("en");
                seller.setPreferredCurrency("USD");
                seller.setMarketingOptIn(true);
                seller.setStatus(UserStatus.ACTIVE);
                seller.setRoles(Set.of(RoleName.ROLE_SELLER));
                seller = userProfileRepository.save(seller);

                SellerProfile sellerProfile = new SellerProfile();
                sellerProfile.setUuid("sell-0000-0000-0000-000000000001");
                sellerProfile.setUserProfile(seller);
                sellerProfile.setStoreName("Bob Tech & Gadgets");
                sellerProfile.setStoreSlug("bob-tech-gadgets");
                sellerProfile.setStoreDescription("Official authorized retailer of consumer electronics, smart home devices, and premium accessories.");
                sellerProfile.setStoreLogoUrl("https://cdn.ecomm.com/stores/bob-tech/logo.png");
                sellerProfile.setStoreBannerUrl("https://cdn.ecomm.com/stores/bob-tech/banner.png");
                sellerProfile.setBusinessName("Bob Merchant Electronics LLC");
                sellerProfile.setBusinessRegistrationNumber("REG-US-9948271");
                sellerProfile.setTaxId("EIN-84-2910482");
                sellerProfile.setSupportEmail("support@bobtech.ecomm.com");
                sellerProfile.setSupportPhone("+18005550199");
                sellerProfile.setReturnPolicy("30-day hassle-free returns on all unopened and defective electronics.");
                sellerProfile.setShippingPolicy("Free 2-day expedited shipping on orders over $50; ships within 24 hours.");
                sellerProfile.setVerificationStatus(SellerVerificationStatus.VERIFIED);
                sellerProfile.setVerifiedAt(Instant.now());
                sellerProfileRepository.save(sellerProfile);

                log.info("Initialized default seller profile and storefront for seller@ecomm.com");
            }
        };
    }
}

