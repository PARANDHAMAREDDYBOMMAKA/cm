package com.claimguard.fhir;

import ca.uhn.fhir.context.FhirContext;
import com.claimguard.claim.Claim;
import com.claimguard.extraction.DocumentExtraction;
import com.claimguard.extraction.ExtractionLineItem;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
public class FhirClaimMapper {

    private static final String DEFAULT_CURRENCY = "INR";
    private static final String CLAIM_TYPE_SYSTEM = "http://terminology.hl7.org/CodeSystem/claim-type";

    private final FhirContext fhir;

    public FhirClaimMapper(FhirContext fhir) {
        this.fhir = fhir;
    }

    public String toJson(Claim claim, List<DocumentExtraction> extractions) {
        return fhir.newJsonParser().setPrettyPrint(true).encodeResourceToString(toBundle(claim, extractions));
    }

    public Bundle toBundle(Claim claim, List<DocumentExtraction> extractions) {
        DocumentExtraction primary = extractions.stream()
                .filter(extraction -> extraction.getTotalAmount() != null)
                .findFirst()
                .orElseGet(() -> extractions.isEmpty() ? null : extractions.get(0));

        Patient patient = patient(claim, primary);
        Organization provider = provider(primary);
        Coverage coverage = coverage(patient);

        Bundle bundle = new Bundle();
        bundle.setId(UUID.randomUUID().toString());
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTimestamp(new Date());
        addEntry(bundle, patient);
        addEntry(bundle, provider);
        addEntry(bundle, coverage);
        addEntry(bundle, claimResource(claim, primary, patient, provider, coverage));
        return bundle;
    }

    private static Patient patient(Claim claim, DocumentExtraction extraction) {
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID().toString());
        String name = extraction != null && extraction.getPatientName() != null
                ? extraction.getPatientName()
                : claim.getClaimantName();
        if (name != null) {
            patient.addName().setText(name);
        }
        if (extraction != null) {
            if (extraction.getPatientId() != null) {
                patient.addIdentifier().setValue(extraction.getPatientId());
            }
            gender(extraction.getPatientGender()).ifPresent(patient::setGender);
        }
        return patient;
    }

    private static Organization provider(DocumentExtraction extraction) {
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID().toString());
        if (extraction == null) {
            return organization;
        }
        if (extraction.getProviderName() != null) {
            organization.setName(extraction.getProviderName());
        }
        if (extraction.getProviderAddress() != null) {
            organization.addAddress().setText(extraction.getProviderAddress());
        }
        return organization;
    }

    private static Coverage coverage(Patient patient) {
        Coverage coverage = new Coverage();
        coverage.setId(UUID.randomUUID().toString());
        coverage.setStatus(Coverage.CoverageStatus.ACTIVE);
        coverage.setBeneficiary(reference(patient.getId(), "Patient"));
        return coverage;
    }

    private static org.hl7.fhir.r4.model.Claim claimResource(Claim claim,
            DocumentExtraction extraction,
            Patient patient,
            Organization provider,
            Coverage coverage) {
        org.hl7.fhir.r4.model.Claim resource = new org.hl7.fhir.r4.model.Claim();
        resource.setId(claim.getId().toString());
        resource.addIdentifier().setValue(claim.getReference());
        resource.setStatus(org.hl7.fhir.r4.model.Claim.ClaimStatus.ACTIVE);
        resource.setUse(org.hl7.fhir.r4.model.Claim.Use.CLAIM);
        resource.setType(new CodeableConcept().addCoding(coding(CLAIM_TYPE_SYSTEM, "institutional")));
        resource.setCreated(Date.from(claim.getCreatedAt()));
        resource.setPatient(reference(patient.getId(), "Patient"));
        resource.setProvider(reference(provider.getId(), "Organization"));
        resource.addInsurance()
                .setSequence(1)
                .setFocal(true)
                .setCoverage(reference(coverage.getId(), "Coverage"));

        if (extraction == null) {
            return resource;
        }

        if (extraction.getInvoiceNumber() != null) {
            resource.addIdentifier().setValue(extraction.getInvoiceNumber());
        }
        if (extraction.getDiagnosis() != null) {
            resource.addDiagnosis()
                    .setSequence(1)
                    .setDiagnosis(new CodeableConcept().setText(extraction.getDiagnosis()));
        }
        if (extraction.getAdmissionDate() != null) {
            resource.getBillablePeriod().setStart(toDate(extraction.getAdmissionDate()));
        }
        if (extraction.getDischargeDate() != null) {
            resource.getBillablePeriod().setEnd(toDate(extraction.getDischargeDate()));
        }

        String currency = extraction.getCurrency() != null ? extraction.getCurrency() : DEFAULT_CURRENCY;
        int sequence = 1;
        for (ExtractionLineItem item : extraction.getLineItems()) {
            addItem(resource.addItem().setSequence(sequence++), item, currency);
        }
        if (extraction.getTotalAmount() != null) {
            resource.setTotal(money(extraction.getTotalAmount(), currency));
        }
        return resource;
    }

    private static void addItem(org.hl7.fhir.r4.model.Claim.ItemComponent component,
            ExtractionLineItem item,
            String currency) {
        component.setProductOrService(new CodeableConcept().setText(
                item.getDescription() != null ? item.getDescription() : "Unspecified"));
        if (item.getCode() != null) {
            component.getProductOrService().addCoding(coding(null, item.getCode()));
        }
        if (item.getQuantity() != null) {
            component.getQuantity().setValue(item.getQuantity());
        }
        if (item.getUnitAmount() != null) {
            component.setUnitPrice(money(item.getUnitAmount(), currency));
        }
        if (item.getAmount() != null) {
            component.setNet(money(item.getAmount(), currency));
        }
    }

    private static void addEntry(Bundle bundle, Resource resource) {
        bundle.addEntry()
                .setFullUrl("urn:uuid:" + resource.getIdElement().getIdPart())
                .setResource(resource);
    }

    private static Reference reference(String id, String type) {
        return new Reference("urn:uuid:" + id).setType(type);
    }

    private static Coding coding(String system, String code) {
        Coding coding = new Coding().setCode(code);
        return system != null ? coding.setSystem(system) : coding;
    }

    private static Money money(BigDecimal amount, String currency) {
        return new Money().setValue(amount).setCurrency(currency);
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private static Optional<Enumerations.AdministrativeGender> gender(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ENGLISH);
        if (normalized.startsWith("m")) {
            return Optional.of(Enumerations.AdministrativeGender.MALE);
        }
        if (normalized.startsWith("f")) {
            return Optional.of(Enumerations.AdministrativeGender.FEMALE);
        }
        return Optional.of(Enumerations.AdministrativeGender.OTHER);
    }
}
