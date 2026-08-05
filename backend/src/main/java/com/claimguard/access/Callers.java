package com.claimguard.access;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class Callers {

    private static final String ANONYMOUS = "anonymousUser";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String SCOPE_PREFIX = "SCOPE_";

    private Callers() {
    }

    public static CallerIdentity current(String tenantClaim, String rolesClaim) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || ANONYMOUS.equals(authentication.getName())) {
            return CallerIdentity.anonymous();
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return new CallerIdentity(
                    jwt.getSubject(),
                    stringClaim(jwt, tenantClaim),
                    merge(rolesOf(jwt, rolesClaim), authorities(authentication)),
                    true);
        }
        return new CallerIdentity(authentication.getName(), null, authorities(authentication), true);
    }

    private static String stringClaim(Jwt jwt, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Object value = jwt.getClaim(name);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        if (value instanceof Map<?, ?> map && !map.isEmpty()) {
            Object first = map.keySet().iterator().next();
            return first == null ? null : String.valueOf(first);
        }
        if (value instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            return first == null ? null : String.valueOf(first);
        }
        return null;
    }

    private static Set<String> rolesOf(Jwt jwt, String rolesClaim) {
        Set<String> roles = new LinkedHashSet<>();
        collectRoles(jwt.getClaim(rolesClaim), roles);
        collectRoles(jwt.getClaim("roles"), roles);
        collectRoles(jwt.getClaim("groups"), roles);
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> map) {
            collectRoles(map.get("roles"), roles);
        }
        return roles;
    }

    private static void collectRoles(Object value, Set<String> into) {
        if (value instanceof Map<?, ?> map) {
            map.keySet().forEach(key -> add(into, key));
            return;
        }
        if (value instanceof Iterable<?> items) {
            items.forEach(item -> add(into, item));
            return;
        }
        add(into, value);
    }

    private static Set<String> authorities(Authentication authentication) {
        Set<String> roles = new LinkedHashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value == null) {
                continue;
            }
            if (value.startsWith(ROLE_PREFIX)) {
                add(roles, value.substring(ROLE_PREFIX.length()));
            } else if (!value.startsWith(SCOPE_PREFIX)) {
                add(roles, value);
            }
        }
        return roles;
    }

    private static Set<String> merge(Set<String> first, Set<String> second) {
        Set<String> merged = new LinkedHashSet<>(first);
        merged.addAll(second);
        return merged;
    }

    private static void add(Set<String> into, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).trim().toLowerCase(Locale.ENGLISH);
        if (!text.isEmpty()) {
            into.add(text);
        }
    }
}
