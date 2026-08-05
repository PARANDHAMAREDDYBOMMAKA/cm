package com.claimguard.access;

import java.util.Set;

public record CallerIdentity(String subject, String organisation, Set<String> roles, boolean authenticated) {

    public static CallerIdentity anonymous() {
        return new CallerIdentity(null, null, Set.of(), false);
    }

    public boolean hasAnyRole(Set<String> required) {
        for (String role : required) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
