package com.kaankaplan.emailService.storage;

import com.kaankaplan.emailService.config.storage.EmailArchiveStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailArchiveStorageService {

    private final EmailArchiveStorageProperties properties;
    private final ObjectProvider<S3Client> s3ClientProvider;

    public void archiveTicketEmail(String ticketReference, String recipient, String subject, String html) {
        if (!properties.isEnabled()) {
            return;
        }

        if (!StringUtils.hasText(properties.getBucket())) {
            log.warn("Ticket email archiving is enabled but no archive bucket is configured.");
            return;
        }

        S3Client s3Client = s3ClientProvider.getIfAvailable();
        if (s3Client == null) {
            log.warn("Ticket email archiving is enabled but the S3 client is not configured.");
            return;
        }

        String archiveReference = StringUtils.hasText(ticketReference) ? ticketReference.trim() : UUID.randomUUID().toString();
        String objectKey = buildObjectKey(archiveReference);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .contentType("text/html")
                .metadata(Map.of(
                        "ticket-reference", archiveReference,
                        "recipient", sanitizeMetadataValue(recipient),
                        "subject", sanitizeMetadataValue(subject)
                ))
                .build();

        s3Client.putObject(
                putObjectRequest,
                RequestBody.fromBytes(html.getBytes(StandardCharsets.UTF_8))
        );

        log.info("Archived ticket email for {} at s3://{}/{}", recipient, properties.getBucket(), objectKey);
    }

    private String buildObjectKey(String ticketReference) {
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        String prefix = StringUtils.hasText(properties.getKeyPrefix())
                ? trimSlashes(properties.getKeyPrefix())
                : "ticket-emails";

        return prefix + "/" + now.getYear() + "/" + pad(now.getMonthValue()) + "/" + pad(now.getDayOfMonth()) + "/" + ticketReference + ".html";
    }

    private String pad(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private String trimSlashes(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String sanitizeMetadataValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }

        return value.replaceAll("[\\r\\n]+", " ").trim();
    }
}
