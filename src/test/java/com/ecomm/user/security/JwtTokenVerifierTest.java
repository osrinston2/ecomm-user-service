package com.ecomm.user.security;

import com.ecomm.user.config.JwtConfigProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenVerifierTest {

    private ObjectMapper objectMapper;
    private JwksPublicKeyProvider publicKeyProvider;
    private JwtTokenVerifier jwtTokenVerifier;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        JwtConfigProperties config = new JwtConfigProperties();
        config.setJwksUri("");
        config.setPublicKeyUri("");

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        publicKey = (RSAPublicKey) keyPair.getPublic();

        publicKeyProvider = new JwksPublicKeyProvider(config, objectMapper);
        publicKeyProvider.setPublicKeyForTesting(publicKey, "test-kid-1");
        jwtTokenVerifier = new JwtTokenVerifier(publicKeyProvider, objectMapper, config);
    }

    @Test
    void validateAndParseClaims_InvalidIssuer_ThrowsSecurityException() throws Exception {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", "untrusted-issuer");
        claims.put("sub", "c0000000-0000-0000-0000-000000000001");
        claims.put("exp", now + 900);

        String token = signJwt(claims);

        SecurityException ex = assertThrows(SecurityException.class,
            () -> jwtTokenVerifier.validateAndParseClaims(token));
        assertTrue(ex.getMessage().contains("Invalid JWT issuer"));
    }

    @Test
    void validateAndParseClaims_ValidRs256Token_ReturnsClaims() throws Exception {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", "ecomm-auth-service");
        claims.put("sub", "c0000000-0000-0000-0000-000000000001");
        claims.put("email", "customer@ecomm.com");
        claims.put("roles", List.of("ROLE_CUSTOMER"));
        claims.put("token_type", "ACCESS");
        claims.put("jti", "token-jti-123");
        claims.put("iat", now);
        claims.put("exp", now + 900);

        String token = signJwt(claims);

        Map<String, Object> parsed = jwtTokenVerifier.validateAndParseClaims(token);
        assertEquals("c0000000-0000-0000-0000-000000000001", parsed.get("sub"));
        assertEquals("customer@ecomm.com", parsed.get("email"));
        assertEquals("ACCESS", parsed.get("token_type"));
    }

    @Test
    void validateAndParseClaims_ExpiredToken_ThrowsSecurityException() throws Exception {
        long past = Instant.now().getEpochSecond() - 3600;
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", "c0000000-0000-0000-0000-000000000001");
        claims.put("exp", past);

        String token = signJwt(claims);

        SecurityException ex = assertThrows(SecurityException.class,
            () -> jwtTokenVerifier.validateAndParseClaims(token));
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    void validateAndParseClaims_TamperedPayload_ThrowsSecurityException() throws Exception {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", "c0000000-0000-0000-0000-000000000001");
        claims.put("roles", List.of("ROLE_CUSTOMER"));
        claims.put("exp", now + 900);

        String token = signJwt(claims);
        String[] parts = token.split("\\.");

        // Tamper with payload to escalate role to ROLE_ADMIN
        Map<String, Object> tamperedClaims = new LinkedHashMap<>(claims);
        tamperedClaims.put("roles", List.of("ROLE_ADMIN"));
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(objectMapper.writeValueAsBytes(tamperedClaims));

        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThrows(SecurityException.class,
            () -> jwtTokenVerifier.validateAndParseClaims(tamperedToken));
    }

    private String signJwt(Map<String, Object> claims) throws Exception {
        Map<String, Object> header = Map.of("alg", "RS256", "typ", "JWT", "kid", "test-kid-1");
        String h = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(header));
        String p = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(claims));
        String data = h + "." + p;

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(data.getBytes(StandardCharsets.UTF_8));
        String sig = Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
        return data + "." + sig;
    }
}

