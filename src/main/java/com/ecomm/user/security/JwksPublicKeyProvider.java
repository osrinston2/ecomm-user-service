package com.ecomm.user.security;

import com.ecomm.user.config.JwtConfigProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class JwksPublicKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(JwksPublicKeyProvider.class);
    private static final long REFRESH_COOLDOWN_SECONDS = 5;
    private static final int TIMEOUT_MS = 3000;

    private final JwtConfigProperties jwtConfig;
    private final ObjectMapper objectMapper;

    private volatile RSAPublicKey cachedPublicKey;
    private volatile String cachedKeyId;
    private volatile Instant lastRefreshAttempt = Instant.EPOCH;
    private boolean staticKeyConfigured = false;

    public JwksPublicKeyProvider(JwtConfigProperties jwtConfig, ObjectMapper objectMapper) {
        this.jwtConfig = jwtConfig;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        String configuredPem = jwtConfig.getRsa() != null ? jwtConfig.getRsa().getPublicKey() : null;
        if (configuredPem != null && !configuredPem.isBlank()) {
            try {
                this.cachedPublicKey = parsePublicKeyPem(configuredPem);
                this.staticKeyConfigured = true;
                log.info("Initialized static RSA Public Key from configuration");
                return;
            } catch (Exception e) {
                log.warn("Failed to parse configured static RSA public key: {}. Will attempt JWKS fetch.", e.getMessage());
            }
        }

        // Attempt initial non-blocking fetch from ecomm-auth-service if JWKS URI is configured
        if ((jwtConfig.getJwksUri() != null && !jwtConfig.getJwksUri().isBlank())
                || (jwtConfig.getPublicKeyUri() != null && !jwtConfig.getPublicKeyUri().isBlank())) {
            refreshPublicKey(true);
        }
    }

    public RSAPublicKey getPublicKey(String requestedKid) {
        if (cachedPublicKey == null) {
            refreshPublicKey(false);
        } else if (!staticKeyConfigured && requestedKid != null && cachedKeyId != null && !requestedKid.equals(cachedKeyId)) {
            log.info("JWT kid '{}' differs from cached kid '{}'; refreshing JWKS public key", requestedKid, cachedKeyId);
            refreshPublicKey(false);
        }
        return cachedPublicKey;
    }

    public synchronized boolean refreshPublicKey(boolean ignoreCooldown) {
        if (staticKeyConfigured) {
            return cachedPublicKey != null;
        }

        Instant now = Instant.now();
        if (!ignoreCooldown && Duration.between(lastRefreshAttempt, now).getSeconds() < REFRESH_COOLDOWN_SECONDS) {
            return cachedPublicKey != null;
        }
        lastRefreshAttempt = now;

        // 1. Try RFC 7517 JWKS endpoint first
        if (fetchFromJwksEndpoint()) {
            return true;
        }

        // 2. Fallback to PEM public-key endpoint
        return fetchFromPemEndpoint();
    }

    @SuppressWarnings("unchecked")
    private boolean fetchFromJwksEndpoint() {
        String jwksUri = jwtConfig.getJwksUri();
        if (jwksUri == null || jwksUri.isBlank()) {
            return false;
        }
        try {
            String body = httpGet(jwksUri);
            if (body != null && !body.isBlank()) {
                Map<String, Object> jwks = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
                if (keys != null && !keys.isEmpty()) {
                    Map<String, Object> jwk = keys.get(0);
                    String nBase64 = (String) jwk.get("n");
                    String eBase64 = (String) jwk.get("e");
                    String kid = (String) jwk.get("kid");

                    BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(nBase64));
                    BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(eBase64));

                    RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    this.cachedPublicKey = (RSAPublicKey) kf.generatePublic(spec);
                    this.cachedKeyId = kid;
                    log.info("Successfully fetched and cached RSA public key from JWKS endpoint: {} (kid: {})", jwksUri, kid);
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("Could not fetch JWKS from {}: {}", jwksUri, e.getMessage());
        }
        return false;
    }

    private boolean fetchFromPemEndpoint() {
        String pemUri = jwtConfig.getPublicKeyUri();
        if (pemUri == null || pemUri.isBlank()) {
            return false;
        }
        try {
            String body = httpGet(pemUri);
            if (body != null && !body.isBlank()) {
                this.cachedPublicKey = parsePublicKeyPem(body);
                log.info("Successfully fetched and cached RSA public key from PEM endpoint: {}", pemUri);
                return true;
            }
        } catch (Exception e) {
            log.debug("Could not fetch PEM public key from {}: {}", pemUri, e.getMessage());
        }
        return false;
    }

    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        try {
            int status = conn.getResponseCode();
            if (status == 200) {
                try (InputStream is = conn.getInputStream()) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            return null;
        } finally {
            conn.disconnect();
        }
    }

    public RSAPublicKey parsePublicKeyPem(String pem) throws Exception {
        String cleanPem = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("-----BEGIN RSA PUBLIC KEY-----", "")
            .replace("-----END RSA PUBLIC KEY-----", "")
            .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(cleanPem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }

    public void setPublicKeyForTesting(RSAPublicKey publicKey, String keyId) {
        this.cachedPublicKey = publicKey;
        this.cachedKeyId = keyId;
        this.staticKeyConfigured = true;
    }
}
