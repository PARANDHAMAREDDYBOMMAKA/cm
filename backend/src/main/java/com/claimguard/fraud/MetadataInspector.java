package com.claimguard.fraud;

import com.claimguard.extraction.MimeTypes;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MetadataInspector {

    private static final Logger log = LoggerFactory.getLogger(MetadataInspector.class);
    private static final int MAX_ATTRIBUTES = 40;

    public ForensicsReport inspect(byte[] content, String contentType) {
        String type = MimeTypes.normalize(contentType);
        try {
            if ("application/pdf".equals(type)) {
                return inspectPdf(content);
            }
            if (type.startsWith("image/")) {
                return inspectImage(content);
            }
        } catch (Exception exception) {
            log.warn("Metadata inspection failed for {}: {}", type, exception.getMessage());
        }
        return ForensicsReport.empty();
    }

    private ForensicsReport inspectImage(byte[] content) throws Exception {
        Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(content));
        Map<String, String> attributes = new LinkedHashMap<>();

        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                if (attributes.size() >= MAX_ATTRIBUTES) {
                    break;
                }
                attributes.put(directory.getName() + "/" + tag.getTagName(), truncate(tag.getDescription()));
            }
        }

        ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        ExifSubIFDDirectory subIfd = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

        String software = ifd0 != null ? ifd0.getString(ExifIFD0Directory.TAG_SOFTWARE) : null;
        String make = ifd0 != null ? ifd0.getString(ExifIFD0Directory.TAG_MAKE) : null;
        String model = ifd0 != null ? ifd0.getString(ExifIFD0Directory.TAG_MODEL) : null;

        Date original = subIfd != null ? subIfd.getDateOriginal() : null;
        Date modified = ifd0 != null ? ifd0.getDate(ExifIFD0Directory.TAG_DATETIME) : null;

        return new ForensicsReport(
                !attributes.isEmpty(),
                truncate(software),
                truncate(make),
                truncate(model),
                original != null ? original.toInstant() : null,
                modified != null ? modified.toInstant() : null,
                null,
                attributes);
    }

    private ForensicsReport inspectPdf(byte[] content) throws Exception {
        try (PDDocument document = Loader.loadPDF(content)) {
            PDDocumentInformation info = document.getDocumentInformation();
            Map<String, String> attributes = new LinkedHashMap<>();
            for (String key : info.getMetadataKeys()) {
                if (attributes.size() >= MAX_ATTRIBUTES) {
                    break;
                }
                attributes.put("PDF/" + key, truncate(info.getCustomMetadataValue(key)));
            }
            attributes.put("PDF/PageCount", String.valueOf(document.getNumberOfPages()));

            return new ForensicsReport(
                    info.getCreator() != null || info.getProducer() != null,
                    truncate(info.getCreator()),
                    null,
                    null,
                    toInstant(info.getCreationDate()),
                    toInstant(info.getModificationDate()),
                    truncate(info.getProducer()),
                    attributes);
        }
    }

    private static java.time.Instant toInstant(Calendar calendar) {
        return calendar == null ? null : calendar.toInstant();
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= 255 ? trimmed : trimmed.substring(0, 255);
    }
}
