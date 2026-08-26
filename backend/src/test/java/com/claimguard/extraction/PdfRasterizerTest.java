package com.claimguard.extraction;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfRasterizerTest {

    private static final Path SAMPLE = Path.of("..", "samples", "clean-appendectomy-bill.pdf");

    private final PdfRasterizer rasterizer = new PdfRasterizer(150f, 2, 1600, 1_800_000L, 0.72f);

    @Test
    void staysWithinThePixelBudget() throws IOException {
        PdfRasterizer.Result result = rasterizer.rasterize(Files.readAllBytes(SAMPLE));

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(result.image()));
        assertThat((long) image.getWidth() * image.getHeight()).isLessThanOrEqualTo(1_800_000L);
        assertThat(image.getWidth()).isLessThanOrEqualTo(1600);
        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.totalPages()).isPositive();
        assertThat(result.renderedPages()).isBetween(1, 2);
    }

    @Test
    void keepsTheWholePageStackWithinBudgetWhenSeveralPagesRender() throws IOException {
        PdfRasterizer multi = new PdfRasterizer(150f, 4, 1600, 1_800_000L, 0.72f);

        PdfRasterizer.Result result = multi.rasterize(Files.readAllBytes(SAMPLE));

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(result.image()));
        assertThat((long) image.getWidth() * image.getHeight()).isLessThanOrEqualTo(1_800_000L);
    }

    @Test
    void reportsTruncationWhenThePdfHasMorePagesThanWeRender() throws IOException {
        PdfRasterizer single = new PdfRasterizer(150f, 1, 1600, 1_800_000L, 0.72f);

        PdfRasterizer.Result result = single.rasterize(Files.readAllBytes(SAMPLE));

        assertThat(result.truncated()).isEqualTo(result.totalPages() > 1);
        assertThat(result.renderedPages()).isEqualTo(1);
    }

    @Test
    void rejectsSomethingThatIsNotAPdf() {
        assertThatThrownBy(() -> rasterizer.rasterize("not a pdf".getBytes()))
                .isInstanceOf(RuntimeException.class);
    }
}
