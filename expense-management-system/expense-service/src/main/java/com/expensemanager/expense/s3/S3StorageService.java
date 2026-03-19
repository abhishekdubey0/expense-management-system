package com.expensemanager.expense.s3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class S3StorageService {

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "application/pdf"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiration:3600}")
    private long presignedUrlExpirationSeconds;

    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client    = s3Client;
        this.s3Presigner = s3Presigner;
    }

    /**
     * Uploads a receipt file to S3 and returns the S3 object key.
     * The key (not a public URL) is stored in the DB — presigned URLs are generated on read.
     */
    public String uploadReceipt(MultipartFile file, String expenseUuid) {
        validateFile(file);

        String originalName  = file.getOriginalFilename();
        String extension     = getExtension(originalName);
        String objectKey     = "receipts/" + expenseUuid + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(
                    file.getInputStream(), file.getSize()));

            log.info("Receipt uploaded to S3: {}", objectKey);
            return objectKey;

        } catch (IOException e) {
            log.error("Failed to upload receipt to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload receipt. Please try again.", e);
        }
    }

    /**
     * Generates a time-limited presigned URL for a given S3 object key.
     * Called at read time — never stores public URLs in the DB.
     */
    public String generatePresignedUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return null;
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(presignedUrlExpirationSeconds))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toString();

        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key {}: {}", objectKey, e.getMessage());
            return null;
        }
    }

    /**
     * Deletes a receipt from S3 when an expense is hard-deleted (admin only).
     */
    public void deleteReceipt(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
            log.info("Receipt deleted from S3: {}", objectKey);
        } catch (Exception e) {
            log.error("Failed to delete S3 object {}: {}", objectKey, e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                    "Invalid file type. Allowed: JPEG, PNG, GIF, PDF");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}
