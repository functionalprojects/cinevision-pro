package com.kaankaplan.movieService.config.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cinevision.storage.poster")
public class PosterStorageProperties {

    private boolean enabled = false;
    private String bucket;
    private String region = "us-east-1";
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private boolean pathStyleAccessEnabled = false;
    private String publicBaseUrl;
    private String keyPrefix = "movie-posters";
}
