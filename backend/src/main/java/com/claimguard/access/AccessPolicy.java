package com.claimguard.access;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AccessPolicy {

    private final ClaimScope configuredScope;
    private final String tenantClaim;
    private final String rolesClaim;
    private final Set<String> reviewerRoles;
    private final boolean authConfigured;

    public AccessPolicy(@Value("${CLAIM_SCOPE:org}") String scope,
            @Value("${TENANT_CLAIM:urn:zitadel:iam:user:resourceowner:id}") String tenantClaim,
            @Value("${ROLES_CLAIM:urn:zitadel:iam:org:project:roles}") String rolesClaim,
            @Value("${REVIEWER_ROLES:}") String reviewerRoles,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri) {
        this.configuredScope = ClaimScope.parse(scope);
        this.tenantClaim = tenantClaim;
        this.rolesClaim = rolesClaim;
        this.reviewerRoles = split(reviewerRoles);
        this.authConfigured = issuerUri != null && !issuerUri.isBlank();
    }

    public CallerIdentity caller() {
        return Callers.current(tenantClaim, rolesClaim);
    }

    public ClaimScope scope() {
        CallerIdentity caller = caller();
        if (!authConfigured || !caller.authenticated()) {
            return ClaimScope.NONE;
        }
        if (configuredScope == ClaimScope.ORG && caller.organisation() == null) {
            return ClaimScope.USER;
        }
        return configuredScope;
    }

    public String ownerSubject() {
        return caller().subject();
    }

    public String ownerOrg() {
        return caller().organisation();
    }

    public boolean canSee(String claimOwnerSubject, String claimOwnerOrg) {
        CallerIdentity caller = caller();
        return switch (scope()) {
            case NONE -> true;
            case USER -> claimOwnerSubject == null || claimOwnerSubject.equals(caller.subject());
            case ORG -> claimOwnerOrg == null || claimOwnerOrg.equals(caller.organisation());
        };
    }

    public boolean isReviewer() {
        if (reviewerRoles.isEmpty() || !authConfigured) {
            return true;
        }
        return caller().hasAnyRole(reviewerRoles);
    }

    public void requireReviewer() {
        if (!isReviewer()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This action requires one of these roles: " + String.join(", ", reviewerRoles));
        }
    }

    public Set<String> configuredReviewerRoles() {
        return reviewerRoles;
    }

    private static Set<String> split(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(role -> role.trim().toLowerCase(Locale.ENGLISH))
                .filter(role -> !role.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
