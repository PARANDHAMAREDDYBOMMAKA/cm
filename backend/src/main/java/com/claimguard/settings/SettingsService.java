package com.claimguard.settings;

import com.claimguard.analytics.Analytics;
import com.claimguard.audit.AuditLookup;
import com.claimguard.audit.dto.AuditVerificationResponse;
import com.claimguard.extraction.DocumentReader;
import com.claimguard.fhir.NhcxGateway;
import com.claimguard.fraud.AiImageDetector;
import com.claimguard.fraud.EmbeddingProvider;
import com.claimguard.notify.Notifier;
import com.claimguard.settings.dto.CapabilityResponse;
import com.claimguard.settings.dto.SettingResponse;
import com.claimguard.settings.dto.SettingsResponse;
import com.claimguard.storage.StorageService;
import com.claimguard.storage.UnconfiguredStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SettingsService {

    private final DocumentReader reader;
    private final EmbeddingProvider embeddings;
    private final AiImageDetector aiDetector;
    private final StorageService storage;
    private final Notifier notifier;
    private final Analytics analytics;
    private final NhcxGateway nhcx;
    private final AuditLookup audit;
    private final String issuerUri;
    private final int autoApproveMaxScore;
    private final long slaHours;
    private final String nhcxParticipantCode;
    private final String sentryDsn;

    public SettingsService(DocumentReader reader,
            EmbeddingProvider embeddings,
            AiImageDetector aiDetector,
            StorageService storage,
            Notifier notifier,
            Analytics analytics,
            NhcxGateway nhcx,
            AuditLookup audit,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri,
            @Value("${AUTO_APPROVE_MAX_SCORE:24}") int autoApproveMaxScore,
            @Value("${DECISION_SLA_HOURS:24}") long slaHours,
            @Value("${NHCX_PARTICIPANT_CODE:claimguard.demo@hcx}") String nhcxParticipantCode,
            @Value("${sentry.dsn:}") String sentryDsn) {
        this.reader = reader;
        this.embeddings = embeddings;
        this.aiDetector = aiDetector;
        this.storage = storage;
        this.notifier = notifier;
        this.analytics = analytics;
        this.nhcx = nhcx;
        this.audit = audit;
        this.issuerUri = issuerUri;
        this.autoApproveMaxScore = autoApproveMaxScore;
        this.slaHours = slaHours;
        this.nhcxParticipantCode = nhcxParticipantCode;
        this.sentryDsn = sentryDsn;
    }

    public SettingsResponse snapshot() {
        AuditVerificationResponse verification = audit.verify();
        return new SettingsResponse(
                capabilities(),
                thresholds(),
                verification.intact(),
                verification.eventCount(),
                verification.message());
    }

    private List<CapabilityResponse> capabilities() {
        return List.of(
                new CapabilityResponse("auth", "Identity provider", !issuerUri.isBlank(),
                        issuerUri.isBlank() ? "No issuer configured; every request is allowed." : issuerUri),
                new CapabilityResponse("storage", "Document storage",
                        !(storage instanceof UnconfiguredStorageService),
                        storage instanceof UnconfiguredStorageService
                                ? "Set R2_ENDPOINT, R2_BUCKET and the R2 keys to store uploads."
                                : "Cloudflare R2"),
                new CapabilityResponse("reader", "Document reader", reader.isAvailable(),
                        reader.isAvailable()
                                ? reader.modelName()
                                : "Set CLOUDFLARE_API_TOKEN or GROQ_API_KEY to read documents."),
                new CapabilityResponse("embeddings", "Semantic duplicate check", embeddings.isAvailable(),
                        embeddings.isAvailable()
                                ? embeddings.modelName()
                                : "Set CLOUDFLARE_API_TOKEN to compare claims by meaning."),
                new CapabilityResponse("aiDetector", "AI-generated image detection", aiDetector.isAvailable(),
                        aiDetector.isAvailable()
                                ? aiDetector.name()
                                : "No detector is wired in, so this signal never fires."),
                new CapabilityResponse("notifications", "Review notifications", notifier.isAvailable(),
                        notifier.isAvailable()
                                ? "Resend"
                                : "Set RESEND_API_KEY and NOTIFY_TO to email on flagged claims."),
                new CapabilityResponse("analytics", "Product analytics", analytics.isAvailable(),
                        analytics.isAvailable()
                                ? "PostHog"
                                : "Set POSTHOG_API_KEY to capture claim events."),
                new CapabilityResponse("nhcx", "NHCX exchange", nhcx.isAvailable(),
                        nhcx.isAvailable()
                                ? nhcxParticipantCode
                                : "Bundles are FHIR-ready but nothing is submitted yet."),
                new CapabilityResponse("errorReporting", "Error reporting", !sentryDsn.isBlank(),
                        sentryDsn.isBlank()
                                ? "Set SENTRY_DSN to report backend errors."
                                : "Sentry"),
                new CapabilityResponse("apiDocs", "API documentation", true, "/docs and /redoc"));
    }

    private List<SettingResponse> thresholds() {
        return List.of(
                new SettingResponse("Auto-approve at or below", autoApproveMaxScore + " / 100",
                        "Claims scoring above this are routed to a human. Set AUTO_APPROVE_MAX_SCORE."),
                new SettingResponse("Decision SLA", slaHours + " hours",
                        "Undecided claims older than this count as breached. Set DECISION_SLA_HOURS."),
                new SettingResponse("Flag for review at", "50 / 100",
                        "Risk bands: clean 0, low 1-24, medium 25-49, high 50-74, critical 75+."),
                new SettingResponse("Near-duplicate image distance", "6",
                        "Maximum perceptual hash Hamming distance treated as the same document."),
                new SettingResponse("Semantic duplicate similarity", "0.94",
                        "Minimum embedding cosine similarity treated as matching contents."),
                new SettingResponse("Line-item drift tolerance", "2%",
                        "How far line items may stray from the stated total before it is flagged."));
    }
}
