package com.ecomm.user.security;

import com.ecomm.user.config.JwtConfigProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
public class JwtTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenVerifier.class);

    private final JwksPublicKeyProvider publicKeyProvider;
    private final ObjectMapper objectMapper;
    private final JwtConfigProperties jwtConfig;

    public JwtTokenVerifier(JwksPublicKeyProvider publicKeyProvider, ObjectMapper objectMapper) {
        this(publicKeyProvider, objectMapper, null);
    }

    @Autowired
    public JwtTokenVerifier(JwksPublicKeyProvider publicKeyProvider,
                            ObjectMapper objectMapper,
                            JwtConfigProperties jwtConfig) {
        this.publicKeyProvider = publicKeyProvider;
        this.objectMapper = objectMapper;
        this.jwtConfig = jwtConfig;
    }

    public Map<String, Object> validateAndParseClaims(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new SecurityException("Invalid JWT token format");
        }

        String dataToVerify = parts[0] + "." + parts[1];
        byte[] signatureBytes = base64UrlDecode(parts[2]);

        String kid = extractKeyIdFromHeader(parts[0]);
        RSAPublicKey publicKey = publicKeyProvider.getPublicKey(kid);
        if (publicKey == null) {
            throw new SecurityException("Unable to obtain RSA Public Key from auth-service to verify JWT");
        }

        boolean verified = verifySignature(dataToVerify, signatureBytes, publicKey);
        if (!verified) {
            // Attempt a refresh in case ecomm-auth-service restarted with a new ephemeral key pair
            if (publicKeyProvider.refreshPublicKey(false)) {
                RSAPublicKey refreshedKey = publicKeyProvider.getPublicKey(kid);
                if (refreshedKey != null) {
                    verified = verifySignature(dataToVerify, signatureBytes, refreshedKey);
                }
            }
        }

        if (!verified) {
            throw new SecurityException("JWT signature validation failed");
        }

        try {
            byte[] payloadBytes = base64UrlDecode(parts[1]);
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, new TypeReference<Map<String, Object>>() {});

            Object expObj = claims.get("exp");
            if (expObj == null || Instant.now().getEpochSecond() > ((Number) expObj).longValue()) {
                throw new SecurityException("JWT token is expired");
            }

            // OWASP API2:2023 - Validate JWT issuer claim if configured
            if (jwtConfig != null && jwtConfig.getIssuer() != null && !jwtConfig.getIssuer().isBlank()) {
                String tokenIssuer = (String) claims.get("iss");
                if (tokenIssuer != null && !jwtConfig.getIssuer().equals(tokenIssuer)) {
                    throw new SecurityException("Invalid JWT issuer: " + tokenIssuer);
                }
            }

            return claims;
        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            throw new SecurityException("Failed to parse JWT payload: " + e.getMessage(), e);
        }
    }

    private boolean verifySignature(String dataToVerify, byte[] signatureBytes, RSAPublicKey publicKey) {
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(dataToVerify.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(signatureBytes);
        } catch (Exception e) {
            log.debug("Signature verification exception: {}", e.getMessage());
            return false;
        }
    }

    private String extractKeyIdFromHeader(String encodedHeader) {
        try {
            byte[] headerBytes = base64UrlDecode(encodedHeader);
            Map<String, Object> header = objectMapper.readValue(headerBytes, new TypeReference<Map<String, Object>>() {});
            return (String) header.get("kid");
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] base64UrlDecode(String str) {
        return Base64.getUrlDecoder().decode(str);
    }
}
