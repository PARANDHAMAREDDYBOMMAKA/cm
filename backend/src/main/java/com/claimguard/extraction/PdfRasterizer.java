package com.claimguard.extraction;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class PdfRasterizer {

    private static final String JPEG = "image/jpeg";

    private final float dpi;
    private final int maxPages;
    private final int maxWidth;
    private final long maxPixels;
    private final float quality;

    public PdfRasterizer(float dpi, int maxPages, int maxWidth, long maxPixels, float quality) {
        this.dpi = dpi;
        this.maxPages = maxPages;
        this.maxWidth = maxWidth;
        this.maxPixels = maxPixels;
        this.quality = quality;
    }

    public Result rasterize(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            int total = document.getNumberOfPages();
            if (total == 0) {
                throw new IllegalArgumentException("PDF has no pages");
            }
            int rendered = Math.min(total, maxPages);
            PDFRenderer renderer = new PDFRenderer(document);

            List<BufferedImage> pages = new ArrayList<>(rendered);
            for (int index = 0; index < rendered; index++) {
                pages.add(renderer.renderImageWithDPI(index, dpi, ImageType.RGB));
            }
            BufferedImage combined = fit(stack(pages));
            return new Result(toJpeg(combined), JPEG, total, rendered);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read the PDF", exception);
        }
    }

    private static BufferedImage stack(List<BufferedImage> pages) {
        if (pages.size() == 1) {
            return pages.get(0);
        }
        int width = pages.stream().mapToInt(BufferedImage::getWidth).max().orElse(1);
        int height = pages.stream().mapToInt(BufferedImage::getHeight).sum();

        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = combined.createGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(0, 0, width, height);

        int offset = 0;
        for (BufferedImage page : pages) {
            graphics.drawImage(page, 0, offset, null);
            offset += page.getHeight();
        }
        graphics.dispose();
        return combined;
    }

    private BufferedImage fit(BufferedImage source) {
        long pixels = (long) source.getWidth() * source.getHeight();
        double ratio = 1.0;
        if (source.getWidth() > maxWidth) {
            ratio = (double) maxWidth / source.getWidth();
        }
        if (pixels * ratio * ratio > maxPixels) {
            ratio = Math.sqrt((double) maxPixels / pixels);
        }
        if (ratio >= 1.0) {
            return source;
        }

        int width = Math.max(1, (int) Math.round(source.getWidth() * ratio));
        int height = Math.max(1, (int) Math.round(source.getHeight() * ratio));

        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return scaled;
    }

    private byte[] toJpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(quality);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
        return output.toByteArray();
    }

    public record Result(byte[] image, String contentType, int totalPages, int renderedPages) {

        public boolean truncated() {
            return renderedPages < totalPages;
        }
    }
}
