package com.claimguard.storage;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.net.URI;

public class R2StorageService implements StorageService {

    private final S3Client client;
    private final String bucket;

    public R2StorageService(String endpoint, String bucket, String accessKeyId, String secretAccessKey) {
        this.bucket = bucket;
        this.client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Override
    public StoredObject store(String key, InputStream content, long size, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .build();
        client.putObject(request, RequestBody.fromInputStream(content, size));
        return new StoredObject(key, size);
    }

    @Override
    public RetrievedObject retrieve(String key) {
        ResponseInputStream<GetObjectResponse> response = client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(key).build());
        GetObjectResponse metadata = response.response();
        long size = metadata.contentLength() != null ? metadata.contentLength() : -1;
        return new RetrievedObject(response, metadata.contentType(), size);
    }

    @Override
    public void delete(String key) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
