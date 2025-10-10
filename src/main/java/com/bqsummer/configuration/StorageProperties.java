package com.bqsummer.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class StorageProperties {
    /** type: local | s3 | oss (case-insensitive) */
    private String type = "local";

    // common optional public domain (if set overrides provider based url building)
    private String publicDomain; // e.g. https://cdn.example.com

    // base logical prefix inside bucket or local dir
    private String basePath = "uploads"; // no leading slash

    // Local config
    private Local local = new Local();
    @Data
    public static class Local {
        // root directory for storing files
        private String baseDir = "data/files"; // relative to working dir if not absolute
        private boolean createDirIfMissing = true;
    }

    // S3 config
    private S3 s3 = new S3();
    @Data
    public static class S3 {
        private String bucket;
        private String region;
        private String accessKey;
        private String secretKey;
        private String endpointOverride; // optional custom endpoint
        private String publicDomain; // optional, if set used for publicUrl
    }

    // OSS config
    private Oss oss = new Oss();
    @Data
    public static class Oss {
        private String endpoint; // e.g. https://oss-cn-hangzhou.aliyuncs.com
        private String bucket;
        private String accessKey;
        private String secretKey;
        private String publicDomain; // custom domain / CDN
    }
}
