package com.ecomm.user.controller;

import com.ecomm.user.dto.request.AddressRequest;
import com.ecomm.user.dto.request.SellerProfileRequest;
import com.ecomm.user.dto.request.UpdateProfileRequest;
import com.ecomm.user.entity.AddressType;
import com.ecomm.user.security.JwksPublicKeyProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwksPublicKeyProvider publicKeyProvider;

    private RSAPrivateKey testPrivateKey;

    @BeforeEach
    void setUpKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        testPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
        publicKeyProvider.setPublicKeyForTesting((RSAPublicKey) keyPair.getPublic(), "test-kid-1");
    }

    @Test
    void getMyProfile_AsCustomer_ReturnsGeneralProfileAndAddressBook() throws Exception {
        String customerToken = generateAccessToken(
            "c0000000-0000-0000-0000-000000000001",
            "customer@ecomm.com",
            List.of("ROLE_CUSTOMER")
        );

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("customer@ecomm.com"))
            .andExpect(jsonPath("$.data.firstName").value("Alice"))
            .andExpect(jsonPath("$.data.roles", hasItem("ROLE_CUSTOMER")))
            .andExpect(jsonPath("$.data.addresses", not(empty())))
            .andExpect(jsonPath("$.data.addresses[0].streetLine1").value("742 Evergreen Terrace"))
            .andExpect(jsonPath("$.data.sellerProfile").doesNotExist());
    }

    @Test
    void getMyProfile_AsSeller_ReturnsGeneralProfileAndSellerProfileExtension() throws Exception {
        String sellerToken = generateAccessToken(
            "s0000000-0000-0000-0000-000000000003",
            "seller@ecomm.com",
            List.of("ROLE_SELLER")
        );

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + sellerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("seller@ecomm.com"))
            .andExpect(jsonPath("$.data.roles", hasItem("ROLE_SELLER")))
            .andExpect(jsonPath("$.data.sellerProfile.storeName").value("Bob Tech & Gadgets"))
            .andExpect(jsonPath("$.data.sellerProfile.storeSlug").value("bob-tech-gadgets"))
            .andExpect(jsonPath("$.data.sellerProfile.verificationStatus").value("VERIFIED"))
            .andExpect(jsonPath("$.data.addresses").doesNotExist());
    }

    @Test
    void createAndListAddresses_AsCustomer_Succeeds() throws Exception {
        String customerToken = generateAccessToken(
            "c0000000-0000-0000-0000-000000000001",
            "customer@ecomm.com",
            List.of("ROLE_CUSTOMER")
        );

        AddressRequest newAddress = new AddressRequest();
        newAddress.setLabel("Warehouse");
        newAddress.setRecipientName("Alice Customer");
        newAddress.setPhoneNumber("+1000000001");
        newAddress.setAddressType(AddressType.SHIPPING);
        newAddress.setStreetLine1("500 Market Street");
        newAddress.setCity("San Francisco");
        newAddress.setStateProvince("CA");
        newAddress.setPostalCode("94105");
        newAddress.setCountryCode("US");
        newAddress.setDefaultShipping(true);

        mockMvc.perform(post("/api/v1/users/me/addresses")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAddress)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.label").value("Warehouse"))
            .andExpect(jsonPath("$.data.defaultShipping").value(true));
    }

    @Test
    void updateSellerProfile_AsCustomer_ReturnsForbidden() throws Exception {
        String customerToken = generateAccessToken(
            "c0000000-0000-0000-0000-000000000001",
            "customer@ecomm.com",
            List.of("ROLE_CUSTOMER")
        );

        SellerProfileRequest sellerReq = new SellerProfileRequest();
        sellerReq.setStoreName("Unauthorized Store");

        mockMvc.perform(put("/api/v1/users/me/seller-profile")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellerReq)))
            .andExpect(status().isForbidden());
    }

    @Test
    void getPublicSellerStorefront_WithoutAuth_ReturnsStorefrontAndHidesPrivateTaxId() throws Exception {
        mockMvc.perform(get("/api/v1/sellers/bob-tech-gadgets"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.storeName").value("Bob Tech & Gadgets"))
            .andExpect(jsonPath("$.data.taxId").doesNotExist());
    }

    @Test
    void swaggerAndOpenApiDocs_ArePubliclyAccessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("E-Commerce User Service API"));

        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk());
    }

    @Test
    void searchUsers_AsAdmin_ReturnsPaginatedUserList() throws Exception {
        String adminToken = generateAccessToken(
            "a0000000-0000-0000-0000-000000000002",
            "admin@ecomm.com",
            List.of("ROLE_ADMIN", "ROLE_CUSTOMER")
        );

        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(3)));
    }

    @Test
    void updateMyProfile_AsAuthenticatedUser_UpdatesGeneralProfileFields() throws Exception {
        String customerToken = generateAccessToken(
            "c0000000-0000-0000-0000-000000000001",
            "customer@ecomm.com",
            List.of("ROLE_CUSTOMER")
        );

        UpdateProfileRequest updateReq = new UpdateProfileRequest();
        updateReq.setFirstName("Alice");
        updateReq.setLastName("Wonderland");
        updateReq.setBio("Updated e-commerce bio");
        updateReq.setPreferredCurrency("EUR");

        mockMvc.perform(put("/api/v1/users/me")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.lastName").value("Wonderland"))
            .andExpect(jsonPath("$.data.bio").value("Updated e-commerce bio"))
            .andExpect(jsonPath("$.data.preferredCurrency").value("EUR"));
    }

    private String generateAccessToken(String uuid, String email, List<String> roles) throws Exception {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> header = Map.of("alg", "RS256", "typ", "JWT", "kid", "test-kid-1");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", "ecomm-auth-service-test");
        claims.put("sub", uuid);
        claims.put("email", email);
        claims.put("roles", roles);
        claims.put("token_type", "ACCESS");
        claims.put("jti", "test-jti-" + System.nanoTime());
        claims.put("iat", now);
        claims.put("exp", now + 900);

        String h = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(header));
        String p = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(claims));
        String data = h + "." + p;

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(testPrivateKey);
        signer.update(data.getBytes(StandardCharsets.UTF_8));
        String sig = Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
        return data + "." + sig;
    }
}

