package com.claimguard.web.dto;

public record StackResponse(
        String server,
        String poweredBy,
        String runtime,
        String java,
        String tomcat,
        String servlet,
        String springBoot,
        String spring) {
}
