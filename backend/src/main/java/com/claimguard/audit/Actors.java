package com.claimguard.audit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class Actors {

    public static final String SYSTEM = "system";

    private static final String ANONYMOUS = "anonymousUser";

    private Actors() {
    }

    public static String current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return SYSTEM;
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            return email != null && !email.isBlank() ? email : jwt.getSubject();
        }
        String name = authentication.getName();
        return name == null || name.isBlank() || ANONYMOUS.equals(name) ? SYSTEM : name;
    }
}
