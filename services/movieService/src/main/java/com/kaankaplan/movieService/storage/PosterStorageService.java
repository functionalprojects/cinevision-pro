package com.kaankaplan.movieService.storage;

import com.kaankaplan.movieService.config.storage.PosterStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosterStorageService {

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif",
            "image/svg+xml", "svg"
    );

    private final PosterStorageProperties properties;
    private final ObjectProvider<S3Client> s3ClientProvider;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public String storePoster(String sourceUrl, int movieId) {
        String normalizedSourceUrl = normalizeSourceUrl(sourceUrl);

        if (!properties.isEnabled()) {
            return normalizedSourceUrl;
        }

        if (!StringUtils.hasText(properties.getBucket())) {
            throw new IllegalStateException("Poster storage is enabled but no bucket name is configured.");
        }

        S3Client s3Client = s3ClientProvider.getIfAvailable();
        if (s3Client == null) {
            throw new IllegalStateException("Poster storage is enabled but the S3 client is not configured.");
        }

        DownloadedObject downloadedObject = downloadPoster(normalizedSourceUrl);
        String objectKey = buildObjectKey(movieId, downloadedObject.extension());

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .contentType(downloadedObject.contentType())
                .cacheControl("public, max-age=31536000, immutable")
                .metadata(Map.of("source-url", normalizedSourceUrl))
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(downloadedObject.content()));
        String storedUrl = buildPublicUrl(objectKey);
        log.info("Stored poster for movie {} at {}", movieId, storedUrl);
        return storedUrl;
    }

    private String normalizeSourceUrl(String sourceUrl) {
        if (!StringUtils.hasText(sourceUrl)) {
            throw new IllegalArgumentException("Poster image URL is required.");
        }

        String normalized = sourceUrl.trim();
        URI uri = URI.create(normalized);
        String scheme = uri.getScheme();

        if (!StringUtils.hasText(scheme) || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("Poster image URL must use HTTP or HTTPS.");
        }

        return normalized;
    }

    private DownloadedObject downloadPoster(String sourceUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(sourceUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "CineVision-MovieService/1.0")
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Poster download failed with HTTP status " + response.statusCode() + ".");
            }

            if (response.body() == null || response.body().length == 0) {
                throw new IllegalStateException("Poster download returned an empty file.");
            }

            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .map(value -> value.split(";")[0].trim().toLowerCase(Locale.ROOT))
                    .orElse("application/octet-stream");

            return new DownloadedObject(response.body(), contentType, determineExtension(sourceUrl, contentType));
        } catch (IOException exception) {
            throw new IllegalStateException("Poster download failed due to an I/O error.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Poster download was interrupted.", exception);
        }
    }

    private String determineExtension(String sourceUrl, String contentType) {
        String sanitizedPath = URI.create(sourceUrl).getPath();

        if (StringUtils.hasText(sanitizedPath)) {
            int extensionIndex = sanitizedPath.lastIndexOf('.');
            if (extensionIndex >= 0 && extensionIndex < sanitizedPath.length() - 1) {
                String extension = sanitizedPath.substring(extensionIndex + 1)
                        .replaceAll("[^A-Za-z0-9]", "")
                        .toLowerCase(Locale.ROOT);
                if (StringUtils.hasText(extension)) {
                    return extension;
                }
            }
        }

        return CONTENT_TYPE_TO_EXTENSION.getOrDefault(contentType, "bin");
    }

    private String buildObjectKey(int movieId, String extension) {
        String prefix = StringUtils.hasText(properties.getKeyPrefix())
                ? trimSlashes(properties.getKeyPrefix())
                : "movie-posters";

        return prefix + "/movie-" + movieId + "/" + UUID.randomUUID() + "." + extension;
    }

    private String buildPublicUrl(String objectKey) {
        if (StringUtils.hasText(properties.getPublicBaseUrl())) {
            return trimTrailingSlash(properties.getPublicBaseUrl()) + "/" + objectKey;
        }

        if (StringUtils.hasText(properties.getEndpoint())) {
            return trimTrailingSlash(properties.getEndpoint()) + "/" + properties.getBucket() + "/" + objectKey;
        }

        return "https://" + properties.getBucket() + ".s3." + properties.getRegion() + ".amazonaws.com/" + objectKey;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
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

    private record DownloadedObject(byte[] content, String contentType, String extension) {
    }
}
