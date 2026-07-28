package com.claimguard.extraction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RasterizingDocumentReader implements DocumentReader {

    private static final Logger log = LoggerFactory.getLogger(RasterizingDocumentReader.class);
    private static final String PDF = "application/pdf";

    private final DocumentReader delegate;
    private final PdfRasterizer rasterizer;

    public RasterizingDocumentReader(DocumentReader delegate, PdfRasterizer rasterizer) {
        this.delegate = delegate;
        this.rasterizer = rasterizer;
    }

    @Override
    public boolean isAvailable() {
        return delegate.isAvailable();
    }

    @Override
    public String modelName() {
        return delegate.modelName();
    }

    @Override
    public ExtractedDocument read(byte[] content, String contentType, String filename) {
        if (!PDF.equals(MimeTypes.normalize(contentType))) {
            return delegate.read(content, contentType, filename);
        }
        PdfRasterizer.Result result = rasterizer.rasterize(content);
        if (result.truncated()) {
            log.warn("Rasterized only {} of {} pages of {}", result.renderedPages(), result.totalPages(), filename);
        }
        return delegate.read(result.image(), result.contentType(), filename);
    }
}
