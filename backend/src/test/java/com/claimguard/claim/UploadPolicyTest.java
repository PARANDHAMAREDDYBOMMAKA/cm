package com.claimguard.claim;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class UploadPolicyTest {

    private static byte[] bytes(int... values) {
        byte[] content = new byte[values.length];
        for (int index = 0; index < values.length; index++) {
            content[index] = (byte) values[index];
        }
        return content;
    }

    @Test
    void sniffsPdfFromItsMagicNumber() {
        assertThat(UploadPolicy.sniff("%PDF-1.7\n...".getBytes(StandardCharsets.US_ASCII)))
                .isEqualTo(UploadPolicy.PDF);
    }

    @Test
    void sniffsPngJpegAndTiff() {
        assertThat(UploadPolicy.sniff(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)))
                .isEqualTo(UploadPolicy.PNG);
        assertThat(UploadPolicy.sniff(bytes(0xFF, 0xD8, 0xFF, 0xE0))).isEqualTo(UploadPolicy.JPEG);
        assertThat(UploadPolicy.sniff(bytes(0x49, 0x49, 0x2A, 0x00))).isEqualTo(UploadPolicy.TIFF);
        assertThat(UploadPolicy.sniff(bytes(0x4D, 0x4D, 0x00, 0x2A))).isEqualTo(UploadPolicy.TIFF);
    }

    @Test
    void sniffsWebpOnlyWhenTheRiffChunkSaysWebp() {
        byte[] webp = bytes(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50);
        byte[] wave = bytes(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x41, 0x56, 0x45);

        assertThat(UploadPolicy.sniff(webp)).isEqualTo(UploadPolicy.WEBP);
        assertThat(UploadPolicy.sniff(wave)).isNull();
    }

    @Test
    void rejectsContentThatIsNotAnAcceptedDocument() {
        assertThat(UploadPolicy.sniff("<html><script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)))
                .isNull();
        assertThat(UploadPolicy.sniff("<?xml version=\"1.0\"?><svg/>".getBytes(StandardCharsets.UTF_8)))
                .isNull();
        assertThat(UploadPolicy.sniff(new byte[0])).isNull();
        assertThat(UploadPolicy.sniff(null)).isNull();
    }

    @Test
    void aPdfRenamedToPngIsStillSniffedAsPdf() {
        assertThat(UploadPolicy.sniff("%PDF-1.4".getBytes(StandardCharsets.US_ASCII)))
                .isEqualTo(UploadPolicy.PDF);
    }

    @Test
    void onlyTheAllowlistedTypesAreAccepted() {
        assertThat(UploadPolicy.isAccepted(UploadPolicy.PDF)).isTrue();
        assertThat(UploadPolicy.isAccepted(UploadPolicy.TIFF)).isTrue();
        assertThat(UploadPolicy.isAccepted("text/html")).isFalse();
        assertThat(UploadPolicy.isAccepted("image/svg+xml")).isFalse();
        assertThat(UploadPolicy.isAccepted(null)).isFalse();
    }

    @Test
    void tiffIsNeverServedInlineBecauseBrowsersCannotRenderIt() {
        assertThat(UploadPolicy.isInlineSafe(UploadPolicy.PDF)).isTrue();
        assertThat(UploadPolicy.isInlineSafe(UploadPolicy.PNG)).isTrue();
        assertThat(UploadPolicy.isInlineSafe(UploadPolicy.TIFF)).isFalse();
        assertThat(UploadPolicy.isInlineSafe("text/html")).isFalse();
        assertThat(UploadPolicy.isInlineSafe(null)).isFalse();
    }
}
