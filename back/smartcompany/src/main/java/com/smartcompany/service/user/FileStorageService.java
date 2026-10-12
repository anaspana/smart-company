package com.smartcompany.service.user;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.smartcompany.dto.file.PreSignedUrlDto;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
public class FileStorageService {

    private final String bucketName;
    private final String region;
    private final String accessKeyId;
    private final String secretAccessKey;

    private S3Presigner presigner;

    public FileStorageService(@Value("${aws.region}") String region,
                              @Value("${aws.s3.bucket-name}") String bucketName,
                              @Value("${aws.access-key-id}") String accessKeyId,
                              @Value("${aws.secret-access-key}") String secretAccessKey) {
        this.bucketName = bucketName;
        this.region = region;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
    }

    @PostConstruct
    public void init() {
        this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
    }

    @PreDestroy
    public void destroy() {
        if (this.presigner != null) {
            this.presigner.close();
        }
    }

    /**
     * Generates a presigned PUT URL for uploading a file.
     *
     * @param originalFilename original filename (used to extract extension)
     * @param folder           S3 folder/prefix e.g. "profile-images", "product-images"
     * @return PreSignedUrlDto containing the presigned URL and the generated S3 key
     */
    public PreSignedUrlDto generatePreSignedPutUrl(String originalFilename, String folder) {
        String extension = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String key = folder + "/" + UUID.randomUUID() + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(b -> b
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(putObjectRequest));

        return new PreSignedUrlDto(presignedRequest.url(), key);
    }

    /**
     * Generates a presigned GET URL for downloading/viewing a file.
     *
     * @param key S3 key of the file (e.g. "product-images/uuid.jpg")
     * @return presigned URL valid for 15 minutes
     */
    public URL generatePreSignedGetUrl(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(b -> b
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getObjectRequest));

        return presignedRequest.url();
    }
}