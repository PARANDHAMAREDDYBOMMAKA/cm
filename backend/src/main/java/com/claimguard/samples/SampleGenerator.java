package com.claimguard.samples;

import com.claimguard.fraud.ClinicalReference;
import com.claimguard.fraud.PerceptualHash;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class SampleGenerator {

    record Hospital(String name, String address, String gstin) {
    }

    record Patient(String name, int age, String gender, String uhid) {
    }

    record LineItem(String code, String description, int quantity, BigDecimal rate) {

        BigDecimal amount() {
            return rate.multiply(BigDecimal.valueOf(quantity));
        }
    }

    record Bill(Hospital hospital,
            Patient patient,
            LocalDate admissionDate,
            LocalDate dischargeDate,
            LocalDate invoiceDate,
            String invoiceNumber,
            String diagnosis,
            List<LineItem> lineItems,
            BigDecimal statedTotal) {

        BigDecimal lineItemTotal() {
            return lineItems.stream().map(LineItem::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    record Metadata(String producer, String creator, Instant created, Instant modified) {
    }

    private static final PDFont HELVETICA = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont HELVETICA_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private static final float MARGIN = 50f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();

    private static final Hospital LAKEVIEW = new Hospital("Lakeview Eye & General Hospital",
            "22 Residency Road, Bengaluru, Karnataka 560025", "29AACCL5678G1Z3");
    private static final Hospital SUNRISE = new Hospital("Sunrise Multispeciality Hospital",
            "14 MG Road, Pune, Maharashtra 411001", "27AACCS1234F1Z5");
    private static final Hospital ST_ANNES = new Hospital("St. Anne's Maternity & Surgical Centre",
            "8 Church Street, Chennai, Tamil Nadu 600002", "33AACCA4321H1Z7");
    private static final Hospital GREENFIELD = new Hospital("Greenfield Orthopaedic Institute",
            "45 Ashok Nagar, Hyderabad, Telangana 500020", "36AACCG8765J1Z1");
    private static final List<Hospital> HOSPITALS = List.of(LAKEVIEW, SUNRISE, ST_ANNES, GREENFIELD);

    private static final Patient PATIENT_RAMESH = new Patient("Ramesh Kumar Sharma", 62, "Male", "UHID-2041558");
    private static final Patient PATIENT_SURESH = new Patient("Suresh Babu Reddy", 34, "Male", "UHID-1052390");
    private static final Patient PATIENT_ANJALI = new Patient("Anjali Verma", 29, "Female", "UHID-3087742");
    private static final Patient PATIENT_FATIMA = new Patient("Fatima Sheikh", 27, "Female", "UHID-4098123");
    private static final Patient PATIENT_MEENA = new Patient("Meena Iyer", 45, "Female", "UHID-5061234");
    private static final Patient PATIENT_VIKRAM = new Patient("Vikram Nair", 58, "Male", "UHID-6071234");
    private static final Patient PATIENT_DEVENDRA = new Patient("Devendra Patil", 50, "Male", "UHID-7081234");
    private static final Patient PATIENT_KAVITA = new Patient("Kavita Joshi", 38, "Female", "UHID-8091234");
    private static final Patient PATIENT_ARVIND = new Patient("Arvind Chauhan", 47, "Male", "UHID-9091234");
    private static final List<Patient> PATIENTS = List.of(PATIENT_RAMESH, PATIENT_SURESH, PATIENT_ANJALI,
            PATIENT_FATIMA, PATIENT_MEENA, PATIENT_VIKRAM, PATIENT_DEVENDRA, PATIENT_KAVITA, PATIENT_ARVIND);

    private SampleGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path outputDir = Path.of(args.length > 0 ? args[0] : "../samples");
        Files.createDirectories(outputDir);
        System.out.println("Reference data: " + HOSPITALS.size() + " hospitals, " + PATIENTS.size() + " patients.");
        System.out.println("Writing sample claim documents to " + outputDir.toAbsolutePath().normalize());

        Bill cataract = cleanCataractBill();
        Bill appendectomy = cleanAppendectomyBill();
        Bill delivery = cleanDeliveryBill();

        byte[] cataractPdf = write(outputDir, "clean-cataract-bill.pdf", buildPdf(cataract, cleanMetadata(cataract)),
                "Clean baseline - reads cleanly, sums match, well within the cost band; should auto-approve.");
        write(outputDir, "clean-appendectomy-bill.pdf", buildPdf(appendectomy, cleanMetadata(appendectomy)),
                "Clean baseline - carries invoice number " + appendectomy.invoiceNumber()
                        + ", later reused by fake-reused-invoice.pdf.");
        write(outputDir, "clean-delivery-bill.pdf", buildPdf(delivery, cleanMetadata(delivery)),
                "Clean baseline - reads cleanly, sums match, well within the cost band; should auto-approve.");

        write(outputDir, "fake-exact-duplicate.pdf", cataractPdf,
                "EXACT_DUPLICATE - byte-identical copy of clean-cataract-bill.pdf, same SHA-256.");

        Bill reusedInvoice = fakeReusedInvoiceBill(appendectomy.invoiceNumber());
        write(outputDir, "fake-reused-invoice.pdf", buildPdf(reusedInvoice, cleanMetadata(reusedInvoice)),
                "REUSED_INVOICE_NUMBER - invoice " + appendectomy.invoiceNumber()
                        + " was already claimed on clean-appendectomy-bill.pdf.");

        Bill inflatedTotal = fakeInflatedTotalBill();
        write(outputDir, "fake-inflated-total.pdf", buildPdf(inflatedTotal, cleanMetadata(inflatedTotal)),
                "LINE_ITEM_MISMATCH - line items sum to Rs. " + indianGrouping(inflatedTotal.lineItemTotal())
                        + " but the bill states Rs. " + indianGrouping(inflatedTotal.statedTotal()) + ".");

        Bill outOfBand = fakeAmountOutOfBandBill();
        write(outputDir, "fake-amount-out-of-band.pdf", buildPdf(outOfBand, cleanMetadata(outOfBand)),
                "AMOUNT_OUT_OF_BAND - cataract billed at Rs. " + indianGrouping(outOfBand.statedTotal())
                        + ", far above the usual ceiling.");

        Bill procedureMismatch = fakeProcedureMismatchBill();
        write(outputDir, "fake-procedure-mismatch.pdf", buildPdf(procedureMismatch, cleanMetadata(procedureMismatch)),
                "PROCEDURE_DIAGNOSIS_MISMATCH - diagnosis is cataract but every line item is orthopaedic.");

        Bill dateInconsistency = fakeDateInconsistencyBill();
        write(outputDir, "fake-date-inconsistency.pdf", buildPdf(dateInconsistency, cleanMetadata(dateInconsistency)),
                "DATE_INCONSISTENCY - the discharge date is before the admission date.");

        Bill editedBill = fakeEditedBill();
        Instant created = instantAt(editedBill.invoiceDate());
        Metadata editedMetadata = new Metadata("Adobe Acrobat Pro DC", "Adobe Acrobat Pro DC", created,
                created.plus(5, ChronoUnit.DAYS));
        write(outputDir, "fake-edited-in-acrobat.pdf", buildPdf(editedBill, editedMetadata),
                "EDITING_SOFTWARE + MODIFIED_AFTER_CREATION - Producer/Creator is Adobe Acrobat Pro DC "
                        + "and the file was modified 5 days after it was created.");

        byte[] cleanPng = renderPng(cataractPdf, 150f);
        write(outputDir, "clean-cataract-bill.png", cleanPng,
                "Image variant of the clean cataract bill, exercises the image extraction and hashing path.");

        byte[] nearDuplicatePng = renderPng(cataractPdf, 132f);
        int distance = PerceptualHash.distance(PerceptualHash.of(cleanPng), PerceptualHash.of(nearDuplicatePng));
        write(outputDir, "fake-near-duplicate.png", nearDuplicatePng,
                "NEAR_DUPLICATE_IMAGE - same bill re-rendered at a different scale, perceptual hash Hamming "
                        + "distance " + distance + " from clean-cataract-bill.png (detector threshold is 6).");
    }

    private static Bill cleanCataractBill() {
        List<LineItem> items = List.of(
                new LineItem("PHACO", "Phacoemulsification with IOL Implantation - Right Eye", 1,
                        BigDecimal.valueOf(42_000)),
                new LineItem("IOL01", "Foldable IOL Lens", 1, BigDecimal.valueOf(9_000)),
                new LineItem("OTROOM", "Operation Theatre Charges", 1, BigDecimal.valueOf(6_000)),
                new LineItem("PREOP", "Pre-operative Investigations", 1, BigDecimal.valueOf(2_500)),
                new LineItem("MEDS", "Pharmacy & Consumables", 1, BigDecimal.valueOf(3_200)),
                new LineItem("CONSULT", "Ophthalmologist Consultation", 1, BigDecimal.valueOf(1_500)));
        LocalDate date = LocalDate.of(2026, 7, 10);
        return new Bill(LAKEVIEW, PATIENT_RAMESH, date, date, date, "LEH/2026/07/1042",
                "Cataract (Right Eye) - Immature Senile Cataract", items, BigDecimal.valueOf(64_200));
    }

    private static Bill cleanAppendectomyBill() {
        List<LineItem> items = List.of(
                new LineItem("SURG01", "Laparoscopic Appendectomy", 1, BigDecimal.valueOf(45_000)),
                new LineItem("OTROOM", "Operation Theatre & Anaesthesia Charges", 1, BigDecimal.valueOf(15_000)),
                new LineItem("WARD03", "Ward Stay - 3 days @ Rs 3500/day", 3, BigDecimal.valueOf(3_500)),
                new LineItem("LAB01", "Pre-operative Laboratory Investigations", 1, BigDecimal.valueOf(4_500)),
                new LineItem("MEDS", "Pharmacy & Consumables", 1, BigDecimal.valueOf(6_000)),
                new LineItem("CONSULT", "Surgeon Consultation & Follow-up", 1, BigDecimal.valueOf(3_000)));
        LocalDate admission = LocalDate.of(2026, 6, 18);
        LocalDate discharge = LocalDate.of(2026, 6, 21);
        return new Bill(SUNRISE, PATIENT_SURESH, admission, discharge, discharge, "SMH/2026/06/0876",
                "Acute Appendicitis", items, BigDecimal.valueOf(84_000));
    }

    private static Bill cleanDeliveryBill() {
        List<LineItem> items = List.of(
                new LineItem("DEL01", "Normal Delivery Charges", 1, BigDecimal.valueOf(25_000)),
                new LineItem("LABOR", "Labour Room & Monitoring", 1, BigDecimal.valueOf(8_000)),
                new LineItem("WARD02", "Ward Stay - 2 days @ Rs 3000/day", 2, BigDecimal.valueOf(3_000)),
                new LineItem("NEO01", "New-born Care & Paediatric Charges", 1, BigDecimal.valueOf(6_000)),
                new LineItem("LAB02", "Laboratory Investigations", 1, BigDecimal.valueOf(4_000)),
                new LineItem("MEDS", "Pharmacy & Consumables", 1, BigDecimal.valueOf(3_500)));
        LocalDate admission = LocalDate.of(2026, 5, 2);
        LocalDate discharge = LocalDate.of(2026, 5, 5);
        return new Bill(ST_ANNES, PATIENT_ANJALI, admission, discharge, discharge, "SAMC/2026/05/0231",
                "Full Term Normal Vaginal Delivery", items, BigDecimal.valueOf(52_500));
    }

    private static Bill fakeReusedInvoiceBill(String reusedInvoiceNumber) {
        List<LineItem> items = List.of(
                new LineItem("ORIF01", "ORIF with Plating - Distal Radius", 1, BigDecimal.valueOf(55_000)),
                new LineItem("OTROOM", "Operation Theatre Charges", 1, BigDecimal.valueOf(12_000)),
                new LineItem("IMPLANT", "Orthopaedic Implant - Plate & Screws", 1, BigDecimal.valueOf(18_000)),
                new LineItem("WARD02", "Ward Stay - 2 days @ Rs 3000/day", 2, BigDecimal.valueOf(3_000)),
                new LineItem("LAB03", "Pre-operative Investigations", 1, BigDecimal.valueOf(3_500)),
                new LineItem("MEDS", "Pharmacy & Consumables", 1, BigDecimal.valueOf(2_500)));
        LocalDate admission = LocalDate.of(2026, 7, 20);
        LocalDate discharge = LocalDate.of(2026, 7, 23);
        return new Bill(GREENFIELD, PATIENT_FATIMA, admission, discharge, discharge, reusedInvoiceNumber,
                "Fracture - Distal Radius", items, BigDecimal.valueOf(97_000));
    }

    private static Bill fakeInflatedTotalBill() {
        List<LineItem> items = List.of(
                new LineItem("CHOLE1", "Laparoscopic Cholecystectomy", 1, BigDecimal.valueOf(35_000)),
                new LineItem("OTROOM", "Operation Theatre Charges", 1, BigDecimal.valueOf(8_000)),
                new LineItem("WARD02", "Ward Stay - 2 days @ Rs 2500/day", 2, BigDecimal.valueOf(2_500)),
                new LineItem("LAB04", "Pre-operative Investigations", 1, BigDecimal.valueOf(3_000)),
                new LineItem("MEDS", "Pharmacy & Consumables", 1, BigDecimal.valueOf(2_000)));
        LocalDate admission = LocalDate.of(2026, 6, 5);
        LocalDate discharge = LocalDate.of(2026, 6, 7);
        return new Bill(SUNRISE, PATIENT_MEENA, admission, discharge, discharge, "SMH/2026/06/1290",
                "Gallstone Disease - Cholelithiasis with Chronic Cholecystitis", items, BigDecimal.valueOf(140_000));
    }

    private static Bill fakeAmountOutOfBandBill() {
        ClinicalReference.Condition condition = ClinicalReference.match("Cataract").orElseThrow();
        BigDecimal total = condition.ceiling().multiply(BigDecimal.valueOf(7));
        List<LineItem> items = List.of(
                new LineItem("PHACO", "Phacoemulsification with Premium IOL - Left Eye", 1,
                        BigDecimal.valueOf(350_000)),
                new LineItem("IOL01", "Premium Multifocal IOL Lens", 1, BigDecimal.valueOf(40_000)),
                new LineItem("OTROOM", "Operation Theatre Charges", 1, BigDecimal.valueOf(20_000)),
                new LineItem("MEDS", "Pharmacy & Consumables", 1, BigDecimal.valueOf(10_000)));
        LocalDate date = LocalDate.of(2026, 7, 15);
        return new Bill(LAKEVIEW, PATIENT_VIKRAM, date, date, date, "LEH/2026/07/1150",
                "Cataract (Left Eye) - Mature Cataract", items, total);
    }

    private static Bill fakeProcedureMismatchBill() {
        List<LineItem> items = List.of(
                new LineItem("ORIF02", "ORIF with Plating - Tibia", 1, BigDecimal.valueOf(40_000)),
                new LineItem("IMPLANT", "Orthopaedic Implant - Intramedullary Nail", 1, BigDecimal.valueOf(20_000)),
                new LineItem("OTROOM", "Operation Theatre Charges", 1, BigDecimal.valueOf(10_000)),
                new LineItem("WARD02", "Ward Stay - 2 days @ Rs 3000/day", 2, BigDecimal.valueOf(3_000)));
        LocalDate admission = LocalDate.of(2026, 7, 5);
        LocalDate discharge = LocalDate.of(2026, 7, 7);
        return new Bill(GREENFIELD, PATIENT_DEVENDRA, admission, discharge, discharge, "GOI/2026/07/0455",
                "Cataract (Right Eye) - Immature Cataract", items, BigDecimal.valueOf(76_000));
    }

    private static Bill fakeDateInconsistencyBill() {
        List<LineItem> items = List.of(
                new LineItem("SURG01", "Laparoscopic Appendectomy", 1, BigDecimal.valueOf(46_000)),
                new LineItem("OTROOM", "Operation Theatre & Anaesthesia Charges", 1, BigDecimal.valueOf(14_000)),
                new LineItem("WARD02", "Ward Stay - 2 days @ Rs 3500/day", 2, BigDecimal.valueOf(3_500)),
                new LineItem("LAB01", "Pre-operative Laboratory Investigations", 1, BigDecimal.valueOf(4_000)),
                new LineItem("MEDS", "Pharmacy & Consumables", 1, BigDecimal.valueOf(5_500)),
                new LineItem("CONSULT", "Surgeon Consultation & Follow-up", 1, BigDecimal.valueOf(3_000)));
        LocalDate admission = LocalDate.of(2026, 7, 25);
        LocalDate discharge = LocalDate.of(2026, 7, 22);
        LocalDate invoiceDate = LocalDate.of(2026, 7, 25);
        return new Bill(SUNRISE, PATIENT_KAVITA, admission, discharge, invoiceDate, "SMH/2026/07/0999",
                "Acute Appendicitis", items, BigDecimal.valueOf(79_500));
    }

    private static Bill fakeEditedBill() {
        List<LineItem> items = List.of(
                new LineItem("PHACO", "Phacoemulsification with IOL Implantation - Right Eye", 1,
                        BigDecimal.valueOf(42_000)),
                new LineItem("IOL01", "Foldable IOL Lens", 1, BigDecimal.valueOf(9_000)),
                new LineItem("OTROOM", "Operation Theatre Charges", 1, BigDecimal.valueOf(6_000)),
                new LineItem("PREOP", "Pre-operative Investigations", 1, BigDecimal.valueOf(2_500)),
                new LineItem("MEDS", "Pharmacy & Consumables", 1, BigDecimal.valueOf(3_200)),
                new LineItem("CONSULT", "Ophthalmologist Consultation", 1, BigDecimal.valueOf(1_500)));
        LocalDate date = LocalDate.of(2026, 7, 12);
        return new Bill(LAKEVIEW, PATIENT_ARVIND, date, date, date, "LEH/2026/07/1099",
                "Cataract (Right Eye) - Immature Senile Cataract", items, BigDecimal.valueOf(64_200));
    }

    private static Metadata cleanMetadata(Bill bill) {
        Instant issued = instantAt(bill.invoiceDate());
        return new Metadata("ClaimGuard Hospital Billing System", bill.hospital().name(), issued, issued);
    }

    private static Instant instantAt(LocalDate date) {
        return date.atTime(10, 0).toInstant(ZoneOffset.UTC);
    }

    private static byte[] buildPdf(Bill bill, Metadata metadata) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                renderContent(stream, bill);
            }
            applyMetadata(document, metadata);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }

    private static void applyMetadata(PDDocument document, Metadata metadata) {
        PDDocumentInformation info = document.getDocumentInformation();
        info.setProducer(metadata.producer());
        info.setCreator(metadata.creator());
        info.setCreationDate(calendar(metadata.created()));
        info.setModificationDate(calendar(metadata.modified()));
    }

    private static Calendar calendar(Instant instant) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTime(Date.from(instant));
        return calendar;
    }

    private static void renderContent(PDPageContentStream stream, Bill bill) throws IOException {
        float x = MARGIN;
        float xRight = PAGE_WIDTH - MARGIN;
        float y = PAGE_HEIGHT - MARGIN;

        text(stream, HELVETICA_BOLD, 16, x, y, bill.hospital().name());
        y -= 16;
        text(stream, HELVETICA, 9, x, y, bill.hospital().address());
        y -= 12;
        text(stream, HELVETICA, 9, x, y, "GSTIN: " + bill.hospital().gstin());
        y -= 18;
        rule(stream, x, y, xRight, y);
        y -= 18;
        text(stream, HELVETICA_BOLD, 12, x, y, "PATIENT DISCHARGE SUMMARY & TAX INVOICE");
        y -= 22;

        float columnTwo = 320;
        text(stream, HELVETICA, 10, x, y, "Patient Name: " + bill.patient().name());
        text(stream, HELVETICA, 10, columnTwo, y, "UHID: " + bill.patient().uhid());
        y -= 16;
        text(stream, HELVETICA, 10, x, y, "Age / Gender: " + bill.patient().age() + " Yrs / " + bill.patient().gender());
        text(stream, HELVETICA, 10, columnTwo, y, "Invoice No: " + bill.invoiceNumber());
        y -= 16;
        text(stream, HELVETICA, 10, x, y, "Admission Date: " + formatDate(bill.admissionDate()));
        text(stream, HELVETICA, 10, columnTwo, y, "Invoice Date: " + formatDate(bill.invoiceDate()));
        y -= 16;
        text(stream, HELVETICA, 10, x, y, "Discharge Date: " + formatDate(bill.dischargeDate()));
        y -= 16;
        text(stream, HELVETICA, 10, x, y, "Diagnosis: " + bill.diagnosis());
        y -= 24;

        text(stream, HELVETICA_BOLD, 11, x, y, "PARTICULARS OF TREATMENT & CHARGES");
        y -= 14;

        float xCode = 300;
        float xQtyRight = 385;
        float xRateRight = 440;

        rule(stream, x, y, xRight, y);
        y -= 13;
        text(stream, HELVETICA_BOLD, 9, x, y, "Description");
        text(stream, HELVETICA_BOLD, 9, xCode, y, "Code");
        rightAlign(stream, HELVETICA_BOLD, 9, "Qty", xQtyRight, y);
        rightAlign(stream, HELVETICA_BOLD, 9, "Rate (Rs.)", xRateRight, y);
        rightAlign(stream, HELVETICA_BOLD, 9, "Amount (Rs.)", xRight, y);
        y -= 5;
        rule(stream, x, y, xRight, y);
        y -= 14;

        for (LineItem item : bill.lineItems()) {
            text(stream, HELVETICA, 9, x, y, item.description());
            text(stream, HELVETICA, 9, xCode, y, item.code());
            rightAlign(stream, HELVETICA, 9, String.valueOf(item.quantity()), xQtyRight, y);
            rightAlign(stream, HELVETICA, 9, indianGrouping(item.rate()), xRateRight, y);
            rightAlign(stream, HELVETICA, 9, indianGrouping(item.amount()), xRight, y);
            y -= 14;
        }

        rule(stream, x, y, xRight, y);
        y -= 16;
        text(stream, HELVETICA_BOLD, 10, xCode, y, "Total Amount (Rs.)");
        rightAlign(stream, HELVETICA_BOLD, 10, indianGrouping(bill.statedTotal()), xRight, y);
        y -= 6;
        rule(stream, x, y, xRight, y);
        y -= 30;

        text(stream, HELVETICA, 8, x, y, "This is a computer generated bill and does not require a signature.");
    }

    private static void text(PDPageContentStream stream, PDFont font, float size, float x, float y, String value)
            throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        stream.newLineAtOffset(x, y);
        stream.showText(value);
        stream.endText();
    }

    private static void rightAlign(PDPageContentStream stream, PDFont font, float size, String value, float rightX,
            float y) throws IOException {
        float width = font.getStringWidth(value) / 1000 * size;
        text(stream, font, size, rightX - width, y, value);
    }

    private static void rule(PDPageContentStream stream, float x1, float y1, float x2, float y2) throws IOException {
        stream.setLineWidth(0.6f);
        stream.moveTo(x1, y1);
        stream.lineTo(x2, y2);
        stream.stroke();
    }

    private static String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ENGLISH));
    }

    private static String indianGrouping(BigDecimal amount) {
        return indianGrouping(amount.longValueExact());
    }

    private static String indianGrouping(long value) {
        boolean negative = value < 0;
        String digits = Long.toString(Math.abs(value));
        int length = digits.length();
        if (length <= 3) {
            return negative ? "-" + digits : digits;
        }
        StringBuilder groups = new StringBuilder(digits.substring(length - 3));
        int index = length - 3;
        while (index > 0) {
            int start = Math.max(0, index - 2);
            groups.insert(0, digits.substring(start, index) + ",");
            index = start;
        }
        return (negative ? "-" : "") + groups;
    }

    private static byte[] renderPng(byte[] pdf, float dpi) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, dpi, ImageType.RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }

    private static byte[] write(Path dir, String filename, byte[] content, String summary) throws IOException {
        Files.write(dir.resolve(filename), content);
        System.out.println(filename + " (" + content.length + " bytes): " + summary);
        return content;
    }
}
