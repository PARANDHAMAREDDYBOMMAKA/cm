package com.claimguard.extraction;

import com.claimguard.extraction.dto.ExtractionMessage;
import com.claimguard.upstash.QStashVerifier;
import com.claimguard.upstash.UpstashConfig;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Hidden
@RestController
public class ExtractionDeliveryController {

    private final QStashVerifier verifier;
    private final ExtractionService service;
    private final JsonMapper mapper;

    public ExtractionDeliveryController(QStashVerifier verifier, ExtractionService service, JsonMapper mapper) {
        this.verifier = verifier;
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping(UpstashConfig.EXTRACTION_PATH)
    public ResponseEntity<Void> deliver(@RequestHeader(name = "Upstash-Signature", required = false) String signature,
            @RequestBody(required = false) byte[] body) {
        if (!verifier.verify(signature, body)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ExtractionMessage message;
        try {
            message = mapper.readValue(body, ExtractionMessage.class);
        } catch (JacksonException exception) {
            return ResponseEntity.badRequest().build();
        }
        if (message.documentId() == null) {
            return ResponseEntity.badRequest().build();
        }
        service.process(message.documentId());
        return ResponseEntity.noContent().build();
    }
}
