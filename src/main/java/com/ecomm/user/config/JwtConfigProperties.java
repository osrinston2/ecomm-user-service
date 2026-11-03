package com.ecomm.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public class JwtConfigProperties {

    private String issuer = "ecomm-auth-service";
    private String authServiceUrl = "http://localhost:8081";
    private String jwksUri = "http://localhost:8081/api/v1/auth/.well-known/jwks.json";
    private String publicKeyUri = "http://localhost:8081/api/v1/auth/public-key";
    private Rsa rsa = new Rsa();

    public static class Rsa {
        private String publicKey;

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAuthServiceUrl() {
        return authServiceUrl;
    }

    public void setAuthServiceUrl(String authServiceUrl) {
        this.authServiceUrl = authServiceUrl;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String getPublicKeyUri() {
        return publicKeyUri;
    }

    public void setPublicKeyUri(String publicKeyUri) {
        this.publicKeyUri = publicKeyUri;
    }

    public Rsa getRsa() {
        return rsa;
    }

    public void setRsa(Rsa rsa) {
        this.rsa = rsa;
    }
}

