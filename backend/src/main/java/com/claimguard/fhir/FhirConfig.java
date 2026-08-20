package com.claimguard.fhir;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class FhirConfig {

    @Bean
    @Lazy
    FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Bean
    @ConditionalOnMissingBean(NhcxGateway.class)
    NhcxGateway stubNhcxGateway(@Value("${NHCX_PARTICIPANT_CODE:claimguard.demo@hcx}") String participantCode) {
        return new StubNhcxGateway(participantCode);
    }
}
