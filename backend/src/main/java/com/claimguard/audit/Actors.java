package com.claimguard.audit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public final class Actors {

    public static final String SYSTEM = "system";

    private static final String ANONYMOUS = "anonymousUser";

    private static final List<String> IDENTITY_CLAIMS =
            List.of("email", "preferred_username", "name", "given_name", "username");

    private Actors() {
    }

    public static String current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return SYSTEM;
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            for (String claim : IDENTITY_CLAIMS) {
                String value = jwt.getClaimAsString(claim);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return jwt.getSubject();
        }
        String name = authentication.getName();
        return name == null || name.isBlank() || ANONYMOUS.equals(name) ? SYSTEM : name;
    }
}
