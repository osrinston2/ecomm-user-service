package com.ecomm.user.service;

import com.ecomm.user.config.JwtConfigProperties;
import com.ecomm.user.dto.request.UpdateProfileRequest;
import com.ecomm.user.dto.request.UpdateUserStatusRequest;
import com.ecomm.user.dto.response.*;
import com.ecomm.user.entity.*;
import com.ecomm.user.exception.ResourceNotFoundException;
import com.ecomm.user.repository.SellerProfileRepository;
import com.ecomm.user.repository.UserAddressRepository;
import com.ecomm.user.repository.UserProfileRepository;
import com.ecomm.user.security.UserPrincipal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository userProfileRepository;
    private final UserAddressRepository userAddressRepository;
    private final SellerProfileRepository sellerProfileRepository;

    @Autowired(required = false)
    private JwtConfigProperties jwtConfig;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    public UserProfileService(UserProfileRepository userProfileRepository,
                              UserAddressRepository userAddressRepository,
                              SellerProfileRepository sellerProfileRepository) {
        this.userProfileRepository = userProfileRepository;
        this.userAddressRepository = userAddressRepository;
        this.sellerProfileRepository = sellerProfileRepository;
    }

    @Transactional
    public UserPrincipal resolveAndSyncPrincipal(String uuid, String email, List<String> rolesClaim) {
        return resolveAndSyncPrincipal(uuid, email, rolesClaim, null);
    }

    @Transactional
    public UserPrincipal resolveAndSyncPrincipal(String uuid, String email, List<String> rolesClaim, String bearerToken) {
        Set<RoleName> tokenRoles = parseRoles(rolesClaim);

        Optional<UserProfile> byUuid = userProfileRepository.findByUuid(uuid);
        UserProfile profile;

        if (byUuid.isPresent()) {
            profile = byUuid.get();
            boolean updated = false;
            if (email != null && !email.isBlank() && !email.equalsIgnoreCase(profile.getEmail())) {
                profile.setEmail(email);
                updated = true;
            }
            if (!profile.getRoles().containsAll(tokenRoles)) {
                profile.getRoles().addAll(tokenRoles);
                updated = true;
            }
            if (updated) {
                profile = userProfileRepository.save(profile);
            }
        } else if (email != null && !email.isBlank()) {
            Optional<UserProfile> byEmail = userProfileRepository.findByEmailIgnoreCase(email);
            if (byEmail.isPresent()) {
                // Sync UUID with the token's sub claim (e.g. when auth-service generated a fresh UUID for a seeded email)
                profile = byEmail.get();
                log.info("Syncing UserProfile UUID for email {} from {} to {}", email, profile.getUuid(), uuid);
                profile.setUuid(uuid);
                profile.getRoles().addAll(tokenRoles);
                profile = userProfileRepository.save(profile);
            } else {
                profile = createLazyProfile(uuid, email, tokenRoles, bearerToken);
            }
        } else {
            profile = createLazyProfile(uuid, uuid + "@user.ecomm.com", tokenRoles, bearerToken);
        }

        boolean enabled = profile.getStatus() == UserStatus.ACTIVE;
        boolean nonLocked = profile.getStatus() != UserStatus.SUSPENDED && profile.getStatus() != UserStatus.DELETED;

        return new UserPrincipal(
            profile.getUuid(),
            profile.getEmail(),
            profile.getRoles(),
            enabled,
            nonLocked
        );
    }

    private UserProfile createLazyProfile(String uuid, String email, Set<RoleName> roles, String bearerToken) {
        UserProfile profile = new UserProfile();
        profile.setUuid(uuid);
        profile.setEmail(email);

        String localPart = email.contains("@") ? email.substring(0, email.indexOf('@')) : "User";
        String defaultFirstName = localPart.isEmpty() ? "User"
            : Character.toUpperCase(localPart.charAt(0)) + localPart.substring(1);

        profile.setFirstName(defaultFirstName);
        profile.setLastName("Member");
        profile.setStatus(UserStatus.ACTIVE);
        profile.setRoles(new HashSet<>(roles));

        // Attempt to enrich initial firstName/lastName from ecomm-auth-service GET /api/v1/auth/me
        enrichFromAuthServiceMeEndpoint(profile, bearerToken);

        UserProfile saved = userProfileRepository.save(profile);
        log.info("Auto-provisioned UserProfile in ecomm_user_db for uuid={} ({})", uuid, email);
        return saved;
    }

    @SuppressWarnings("unchecked")
    private void enrichFromAuthServiceMeEndpoint(UserProfile profile, String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank() || jwtConfig == null || objectMapper == null) {
            return;
        }
        String authUrl = jwtConfig.getAuthServiceUrl();
        if (authUrl == null || authUrl.isBlank() || (jwtConfig.getJwksUri() != null && jwtConfig.getJwksUri().isBlank())) {
            return;
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(authUrl + "/api/v1/auth/me").toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            try {
                if (conn.getResponseCode() == 200) {
                    try (InputStream is = conn.getInputStream()) {
                        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        Map<String, Object> root = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                        Map<String, Object> data = (Map<String, Object>) root.get("data");
                        if (data != null) {
                            String firstName = (String) data.get("firstName");
                            String lastName = (String) data.get("lastName");
                            if (firstName != null && !firstName.isBlank()) {
                                profile.setFirstName(firstName);
                            }
                            if (lastName != null && !lastName.isBlank()) {
                                profile.setLastName(lastName);
                            }
                        }
                    }
                }
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            log.debug("Could not enrich initial user profile from auth-service /api/v1/auth/me: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByUuid(String uuid) {
        UserProfile profile = userProfileRepository.findByUuid(uuid)
            .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "uuid", uuid));
        return mapToProfileResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(String uuid, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUuid(uuid)
            .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "uuid", uuid));

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            profile.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            profile.setLastName(request.getLastName().trim());
        }
        if (request.getPhoneNumber() != null) {
            profile.setPhoneNumber(request.getPhoneNumber().trim());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl().trim());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender().trim());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio().trim());
        }
        if (request.getPreferredLanguage() != null && !request.getPreferredLanguage().isBlank()) {
            profile.setPreferredLanguage(request.getPreferredLanguage().trim());
        }
        if (request.getPreferredCurrency() != null && !request.getPreferredCurrency().isBlank()) {
            profile.setPreferredCurrency(request.getPreferredCurrency().trim().toUpperCase());
        }
        if (request.getMarketingOptIn() != null) {
            profile.setMarketingOptIn(request.getMarketingOptIn());
        }

        UserProfile saved = userProfileRepository.save(profile);
        return mapToProfileResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserSummaryResponse> searchUsers(UserStatus status,
                                                          RoleName role,
                                                          String search,
                                                          Pageable pageable) {
        Page<UserProfile> page = userProfileRepository.searchUsers(status, role, search, pageable);
        List<UserSummaryResponse> content = page.getContent().stream()
            .map(this::mapToSummaryResponse)
            .collect(Collectors.toList());

        return new PagedResponse<>(
            content,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }

    @Transactional
    public UserProfileResponse updateUserStatus(String uuid, UpdateUserStatusRequest request) {
        UserProfile profile = userProfileRepository.findByUuid(uuid)
            .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "uuid", uuid));

        profile.setStatus(request.getStatus());
        UserProfile saved = userProfileRepository.save(profile);
        log.info("Updated status for user {} ({}) to {} (reason: {})",
            saved.getEmail(), saved.getUuid(), request.getStatus(), request.getReason());
        return mapToProfileResponse(saved);
    }

    public UserProfileResponse mapToProfileResponse(UserProfile profile) {
        UserProfileResponse response = new UserProfileResponse();
        response.setUuid(profile.getUuid());
        response.setEmail(profile.getEmail());
        response.setFirstName(profile.getFirstName());
        response.setLastName(profile.getLastName());
        response.setPhoneNumber(profile.getPhoneNumber());
        response.setAvatarUrl(profile.getAvatarUrl());
        response.setDateOfBirth(profile.getDateOfBirth());
        response.setGender(profile.getGender());
        response.setBio(profile.getBio());
        response.setPreferredLanguage(profile.getPreferredLanguage());
        response.setPreferredCurrency(profile.getPreferredCurrency());
        response.setMarketingOptIn(profile.isMarketingOptIn());
        response.setStatus(profile.getStatus().name());
        response.setRoles(profile.getRoles().stream()
            .map(Enum::name)
            .sorted()
            .collect(Collectors.toList()));
        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());

        // Persona-aware extensions:
        // 1. Customer / Buyer persona sees shipping & billing address book
        List<UserAddress> addresses = userAddressRepository
            .findByUserProfileIdOrderByDefaultShippingDescDefaultBillingDescCreatedAtDesc(profile.getId());
        if (profile.getRoles().contains(RoleName.ROLE_CUSTOMER) || !addresses.isEmpty()) {
            response.setAddresses(addresses.stream()
                .map(AddressService::mapToAddressResponse)
                .collect(Collectors.toList()));
        }

        // 2. Seller / Merchant persona sees merchant storefront profile extension
        Optional<SellerProfile> sellerOpt = sellerProfileRepository.findByUserProfileId(profile.getId());
        if (sellerOpt.isPresent()) {
            response.setSellerProfile(SellerProfileService.mapToSellerResponse(sellerOpt.get(), true));
        } else if (profile.getRoles().contains(RoleName.ROLE_SELLER)) {
            // Return an unconfigured placeholder or empty SellerProfileResponse if seller hasn't set up storefront yet
            SellerProfileResponse placeholder = new SellerProfileResponse();
            placeholder.setOwnerUserUuid(profile.getUuid());
            placeholder.setStoreName(profile.getFirstName() + " " + profile.getLastName() + " Store");
            placeholder.setVerificationStatus(SellerVerificationStatus.PENDING);
            response.setSellerProfile(placeholder);
        }

        return response;
    }

    private UserSummaryResponse mapToSummaryResponse(UserProfile profile) {
        UserSummaryResponse summary = new UserSummaryResponse();
        summary.setUuid(profile.getUuid());
        summary.setEmail(profile.getEmail());
        summary.setFirstName(profile.getFirstName());
        summary.setLastName(profile.getLastName());
        summary.setPhoneNumber(profile.getPhoneNumber());
        summary.setAvatarUrl(profile.getAvatarUrl());
        summary.setStatus(profile.getStatus().name());
        summary.setRoles(profile.getRoles().stream()
            .map(Enum::name)
            .sorted()
            .collect(Collectors.toList()));
        summary.setAddressCount((int) userAddressRepository.countByUserProfileId(profile.getId()));
        summary.setHasSellerProfile(sellerProfileRepository.findByUserProfileId(profile.getId()).isPresent());
        summary.setCreatedAt(profile.getCreatedAt());
        return summary;
    }

    private Set<RoleName> parseRoles(List<String> rolesClaim) {
        Set<RoleName> roles = EnumSet.noneOf(RoleName.class);
        if (rolesClaim != null) {
            for (String r : rolesClaim) {
                try {
                    roles.add(RoleName.valueOf(r));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        if (roles.isEmpty()) {
            roles.add(RoleName.ROLE_CUSTOMER);
        }
        return roles;
    }
}

