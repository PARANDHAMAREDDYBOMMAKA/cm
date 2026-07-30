package com.claimguard.fraud;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ClinicalReference {

    public record Condition(String label,
            List<String> diagnosisKeywords,
            List<String> treatmentKeywords,
            BigDecimal ceiling) {
    }

    private static final List<Condition> CONDITIONS = List.of(
            condition("cataract", List.of("cataract"),
                    List.of("phaco", "iol", "lens", "cataract"), 60_000),
            condition("appendicitis", List.of("appendicitis"),
                    List.of("appendectomy", "appendicectomy", "laparoscop"), 120_000),
            condition("hernia", List.of("hernia"),
                    List.of("hernioplasty", "herniorrhaphy", "mesh", "laparoscop"), 110_000),
            condition("gallstone disease", List.of("cholelithiasis", "gallstone", "cholecystitis"),
                    List.of("cholecystectomy", "laparoscop"), 130_000),
            condition("normal delivery", List.of("normal delivery", "vaginal delivery"),
                    List.of("delivery", "labour", "labor", "episiotomy"), 60_000),
            condition("caesarean delivery", List.of("caesarean", "cesarean", "lscs"),
                    List.of("lscs", "caesarean", "cesarean", "section"), 110_000),
            condition("dengue", List.of("dengue"),
                    List.of("platelet", "fluid", "monitoring", "cbc", "supportive", "ward"), 90_000),
            condition("malaria", List.of("malaria"),
                    List.of("artesunate", "chloroquine", "antimalarial", "fluid", "supportive"), 70_000),
            condition("typhoid", List.of("typhoid", "enteric fever"),
                    List.of("ceftriaxone", "antibiotic", "fluid", "supportive"), 70_000),
            condition("pneumonia", List.of("pneumonia"),
                    List.of("antibiotic", "oxygen", "nebulis", "chest", "supportive"), 120_000),
            condition("myocardial infarction", List.of("myocardial infarction", "stemi", "nstemi", "heart attack"),
                    List.of("angioplasty", "stent", "ptca", "thrombolysis", "cabg", "icu"), 450_000),
            condition("fracture", List.of("fracture"),
                    List.of("orif", "plating", "nailing", "reduction", "cast", "implant"), 200_000),
            condition("renal calculus", List.of("renal calculus", "kidney stone", "nephrolithiasis", "urolithiasis"),
                    List.of("lithotripsy", "eswl", "urs", "pcnl", "stent"), 150_000),
            condition("osteoarthritis", List.of("osteoarthritis"),
                    List.of("arthroplasty", "replacement", "tkr", "physiotherapy"), 350_000),
            condition("covid-19", List.of("covid", "sars-cov-2"),
                    List.of("oxygen", "remdesivir", "isolation", "supportive", "icu"), 250_000));

    private ClinicalReference() {
    }

    public static Optional<Condition> match(String diagnosis) {
        if (diagnosis == null || diagnosis.isBlank()) {
            return Optional.empty();
        }
        String value = diagnosis.toLowerCase(Locale.ENGLISH);
        return CONDITIONS.stream()
                .filter(condition -> condition.diagnosisKeywords().stream().anyMatch(value::contains))
                .findFirst();
    }

    public static boolean isTreated(Condition condition, List<String> descriptions) {
        return descriptions.stream()
                .filter(description -> description != null && !description.isBlank())
                .map(description -> description.toLowerCase(Locale.ENGLISH))
                .anyMatch(description -> condition.treatmentKeywords().stream().anyMatch(description::contains));
    }

    private static Condition condition(String label,
            List<String> diagnosisKeywords,
            List<String> treatmentKeywords,
            long ceiling) {
        return new Condition(label, diagnosisKeywords, treatmentKeywords, BigDecimal.valueOf(ceiling));
    }
}
