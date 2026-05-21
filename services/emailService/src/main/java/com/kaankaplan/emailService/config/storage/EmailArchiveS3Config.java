package com.kaankaplan.emailService.config.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(EmailArchiveStorageProperties.class)
public class EmailArchiveS3Config {

    @Bean
    @ConditionalOnProperty(prefix = "cinevision.storage.email-archive", name = "enabled", havingValue = "true")
    S3Client emailArchiveS3Client(EmailArchiveStorageProperties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyleAccessEnabled())
                        .build());

        if (StringUtils.hasText(properties.getEndpoint())) {
            builder = builder.endpointOverride(URI.create(properties.getEndpoint().trim()));
        }

        if (StringUtils.hasText(properties.getAccessKey()) && StringUtils.hasText(properties.getSecretKey())) {
            builder = builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(properties.getAccessKey().trim(), properties.getSecretKey().trim())
                    )
            );
        } else {
            builder = builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}
