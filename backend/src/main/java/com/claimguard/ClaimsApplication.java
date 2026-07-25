package com.claimguard;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class ClaimsApplication {

    public static void main(String[] args) {
        Map<String, String> environment = loadEnvironment();
        new SpringApplicationBuilder(ClaimsApplication.class)
                .properties(resolveProperties(environment))
                .run(args);
    }

    private static Map<String, Object> resolveProperties(Map<String, String> environment) {
        Map<String, Object> properties = new HashMap<>();
        applyDatasource(properties, environment.get("DATABASE_URL"));
        String issuerUri = environment.get("OIDC_ISSUER_URI");
        if (issuerUri != null && !issuerUri.isBlank()) {
            properties.put("spring.security.oauth2.resourceserver.jwt.issuer-uri", issuerUri.trim());
        }
        String exposeTech = environment.get("APP_EXPOSE_TECH");
        if (exposeTech != null && !exposeTech.isBlank()) {
            properties.put("app.expose-tech", exposeTech.trim());
        }
        return properties;
    }

    private static void applyDatasource(Map<String, Object> properties, String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        URI uri = URI.create(databaseUrl.trim());
        String[] credentials = uri.getUserInfo() == null ? new String[] {""} : uri.getUserInfo().split(":", 2);
        String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
        String query = uri.getQuery() == null ? "" : "?" + uri.getQuery();
        properties.put("spring.datasource.url", "jdbc:postgresql://" + uri.getHost() + port + uri.getPath() + query);
        properties.put("spring.datasource.username", credentials[0]);
        properties.put("spring.datasource.password", credentials.length > 1 ? credentials[1] : "");
    }

    private static Map<String, String> loadEnvironment() {
        Map<String, String> environment = new HashMap<>(System.getenv());
        Path envFile = Path.of(".env");
        if (!Files.exists(envFile)) {
            return environment;
        }
        try {
            for (String line : Files.readAllLines(envFile)) {
                String entry = line.trim();
                if (entry.isEmpty() || entry.startsWith("#")) {
                    continue;
                }
                int separator = entry.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                environment.putIfAbsent(entry.substring(0, separator).trim(), unquote(entry.substring(separator + 1).trim()));
            }
        } catch (IOException ignored) {
        }
        return environment;
    }

    private static String unquote(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
