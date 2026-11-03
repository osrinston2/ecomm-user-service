package com.ecomm.user.security;

import com.ecomm.user.service.TokenBlacklistService;
import com.ecomm.user.service.UserProfileService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenVerifier jwtTokenVerifier;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserProfileService userProfileService;

    public JwtAuthenticationFilter(JwtTokenVerifier jwtTokenVerifier,
                                   TokenBlacklistService tokenBlacklistService,
                                   UserProfileService userProfileService) {
        this.jwtTokenVerifier = jwtTokenVerifier;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userProfileService = userProfileService;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                Map<String, Object> claims = jwtTokenVerifier.validateAndParseClaims(jwt);
                String jti = (String) claims.get("jti");

                // 1. Check shared Redis token blacklist (revoked via auth-service logout)
                if (tokenBlacklistService.isBlacklisted(jti)) {
                    log.debug("Rejected blacklisted JWT jti: {}", jti);
                    filterChain.doFilter(request, response);
                    return;
                }

                // 2. Ensure token is an ACCESS token
                String tokenType = (String) claims.get("token_type");
                if ("ACCESS".equals(tokenType)) {
                    String userUuid = (String) claims.get("sub");
                    String email = (String) claims.get("email");
                    List<String> roles = (List<String>) claims.get("roles");

                    UserPrincipal principal = userProfileService.resolveAndSyncPrincipal(userUuid, email, roles, jwt);
                    if (principal.isEnabled() && principal.isAccountNonLocked()) {
                        UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Could not set user authentication in security context: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}

