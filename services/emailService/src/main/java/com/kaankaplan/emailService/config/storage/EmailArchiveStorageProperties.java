package com.kaankaplan.emailService.config.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cinevision.storage.email-archive")
public class EmailArchiveStorageProperties {

    private boolean enabled = false;
    private String bucket;
    private String region = "us-east-1";
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private boolean pathStyleAccessEnabled = false;
    private String keyPrefix = "ticket-emails";
}
