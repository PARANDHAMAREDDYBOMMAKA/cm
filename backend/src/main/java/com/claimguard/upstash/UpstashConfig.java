package com.claimguard.upstash;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.Arrays;

@Configuration
public class UpstashConfig {

    public static final String EXTRACTION_PATH = "/api/queue/extraction";

    @Bean
    QStashVerifier qstashVerifier(Environment environment, JsonMapper mapper) {
        String publicUrl = environment.getProperty("API_PUBLIC_URL",
                environment.getProperty("RENDER_EXTERNAL_URL", "")).trim();
        if (publicUrl.endsWith("/")) {
            publicUrl = publicUrl.substring(0, publicUrl.length() - 1);
        }
        return new QStashVerifier(
                Arrays.asList(environment.getProperty("QSTASH_CURRENT_SIGNING_KEY"),
                        environment.getProperty("QSTASH_NEXT_SIGNING_KEY")),
                publicUrl.isEmpty() ? null : publicUrl + EXTRACTION_PATH,
                mapper,
                Clock.systemUTC());
    }
}
