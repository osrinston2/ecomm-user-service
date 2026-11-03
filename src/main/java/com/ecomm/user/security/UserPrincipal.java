package com.ecomm.user.security;

import com.ecomm.user.entity.RoleName;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

public class UserPrincipal implements UserDetails {

    private final String uuid;
    private final String email;
    private final Set<RoleName> roleNames;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;
    private final boolean accountNonLocked;

    public UserPrincipal(String uuid,
                         String email,
                         Set<RoleName> roleNames,
                         boolean enabled,
                         boolean accountNonLocked) {
        this.uuid = uuid;
        this.email = email;
        this.roleNames = roleNames != null ? EnumSet.copyOf(roleNames) : EnumSet.of(RoleName.ROLE_CUSTOMER);
        this.authorities = this.roleNames.stream()
            .map(role -> new SimpleGrantedAuthority(role.name()))
            .collect(Collectors.toList());
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
    }

    public static UserPrincipal fromJwtClaims(String uuid, String email, List<String> rolesClaim) {
        Set<RoleName> parsedRoles = EnumSet.noneOf(RoleName.class);
        if (rolesClaim != null) {
            for (String roleStr : rolesClaim) {
                try {
                    parsedRoles.add(RoleName.valueOf(roleStr));
                } catch (IllegalArgumentException ignored) {
                    // Ignore unknown roles
                }
            }
        }
        if (parsedRoles.isEmpty()) {
            parsedRoles.add(RoleName.ROLE_CUSTOMER);
        }
        return new UserPrincipal(uuid, email, parsedRoles, true, true);
    }

    public String getUuid() {
        return uuid;
    }

    public String getEmail() {
        return email;
    }

    public Set<RoleName> getRoleNames() {
        return roleNames;
    }

    public boolean hasRole(RoleName roleName) {
        return roleNames.contains(roleName);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

