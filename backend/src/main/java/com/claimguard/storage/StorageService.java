package com.claimguard.storage;

import java.io.InputStream;

public interface StorageService {

    StoredObject store(String key, InputStream content, long size, String contentType);

    RetrievedObject retrieve(String key);

    void delete(String key);

    record StoredObject(String key, long size) {
    }

    record RetrievedObject(InputStream content, String contentType, long size) {
    }
}
