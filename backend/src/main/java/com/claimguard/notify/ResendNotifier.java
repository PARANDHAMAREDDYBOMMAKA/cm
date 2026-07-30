package com.claimguard.notify;

import com.claimguard.ai.JsonHttpClient;
import com.claimguard.decision.ClaimDecidedEvent;

import java.util.List;
import java.util.Map;

public class ResendNotifier implements Notifier {

    private final JsonHttpClient http;
    private final String apiKey;
    private final String from;
    private final List<String> to;

    public ResendNotifier(JsonHttpClient http, String apiKey, String from, List<String> to) {
        this.http = http;
        this.apiKey = apiKey;
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean isAvailable() {
        return !to.isEmpty();
    }

    @Override
    public void notifyDecision(ClaimDecidedEvent event) {
        http.post("/emails", Map.of(
                "from", from,
                "to", to,
                "subject", subject(event),
                "html", body(event)),
                Map.of("Authorization", "Bearer " + apiKey));
    }

    private static String subject(ClaimDecidedEvent event) {
        return "[ClaimGuard] " + event.reference() + " needs review (risk " + event.riskScore() + "/100)";
    }

    private static String body(ClaimDecidedEvent event) {
        StringBuilder html = new StringBuilder()
                .append("<p>Claim <strong>").append(event.reference()).append("</strong> was routed to review.</p>")
                .append("<p>Outcome: ").append(event.outcome().name())
                .append(" &middot; risk score ").append(event.riskScore()).append("/100")
                .append(" &middot; decided by ").append(event.automatic() ? "the engine" : "a reviewer")
                .append("</p><ul>");
        event.reasons().forEach(reason -> html.append("<li>").append(escape(reason)).append("</li>"));
        return html.append("</ul>").toString();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
