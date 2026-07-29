package com.claimguard.extraction;

import com.claimguard.ai.JsonHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class ExtractionConfig {

    @Bean
    PdfRasterizer pdfRasterizer(
            @Value("${PDF_RENDER_DPI:150}") float dpi,
            @Value("${PDF_MAX_PAGES:2}") int maxPages,
            @Value("${PDF_MAX_WIDTH:1600}") int maxWidth,
            @Value("${PDF_MAX_PIXELS:1800000}") long maxPixels,
            @Value("${PDF_JPEG_QUALITY:0.72}") float quality) {
        return new PdfRasterizer(dpi, maxPages, maxWidth, maxPixels, quality);
    }

    @Bean
    DocumentReader documentReader(Environment environment,
            JsonMapper mapper,
            PdfRasterizer rasterizer,
            @Value("${CLOUDFLARE_MODEL:@cf/meta/llama-4-scout-17b-16e-instruct}") String cloudflareModel,
            @Value("${CLOUDFLARE_BASE_URL:https://api.cloudflare.com}") String cloudflareBaseUrl,
            @Value("${CLOUDFLARE_MAX_TOKENS:4000}") int cloudflareMaxTokens,
            @Value("${GROQ_MODEL:qwen/qwen3.6-27b}") String groqModel,
            @Value("${GROQ_BASE_URL:https://api.groq.com}") String groqBaseUrl,
            @Value("${GROQ_MAX_TOKENS:4500}") int groqMaxTokens,
            @Value("${READER_TIMEOUT_SECONDS:120}") long timeoutSeconds) {
        Duration timeout = Duration.ofSeconds(timeoutSeconds);
        List<DocumentReader> readers = new ArrayList<>();

        String cloudflareToken = environment.getProperty("CLOUDFLARE_API_TOKEN");
        String cloudflareAccount = environment.getProperty("CLOUDFLARE_ACCOUNT_ID");
        if (hasText(cloudflareToken) && hasText(cloudflareAccount)) {
            readers.add(new RasterizingDocumentReader(new OpenAiChatDocumentReader(
                    new JsonHttpClient(cloudflareBaseUrl, timeout, mapper),
                    cloudflareToken,
                    cloudflareModel,
                    "/client/v4/accounts/" + cloudflareAccount + "/ai/v1/chat/completions",
                    cloudflareMaxTokens,
                    Map.of()), rasterizer));
        }

        String groqKey = environment.getProperty("GROQ_API_KEY");
        if (hasText(groqKey)) {
            readers.add(new RasterizingDocumentReader(new OpenAiChatDocumentReader(
                    new JsonHttpClient(groqBaseUrl, timeout, mapper),
                    groqKey,
                    groqModel,
                    "/openai/v1/chat/completions",
                    groqMaxTokens,
                    Map.of("reasoning_format", "hidden")), rasterizer));
        }

        if (readers.isEmpty()) {
            return new UnconfiguredDocumentReader();
        }
        return readers.size() == 1 ? readers.get(0) : new FallbackDocumentReader(readers);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
