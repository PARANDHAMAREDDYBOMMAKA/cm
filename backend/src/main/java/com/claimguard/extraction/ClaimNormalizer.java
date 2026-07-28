package com.claimguard.extraction;

import com.claimguard.extraction.DocumentReader.ExtractedDocument;

public interface ClaimNormalizer {

    ExtractedDocument normalize(ExtractedDocument document);
}
