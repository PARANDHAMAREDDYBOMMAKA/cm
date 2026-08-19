package com.claimguard.storage;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;

public class UnconfiguredStorageService implements StorageService {

    @Override
    public StoredObject store(String key, InputStream content, long size, String contentType) {
        throw notConfigured();
    }

    @Override
    public RetrievedObject retrieve(String key) {
        throw notConfigured();
    }

    @Override
    public void delete(String key) {
        throw notConfigured();
    }

    private ResponseStatusException notConfigured() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Object storage (R2) is not configured");
    }
}
