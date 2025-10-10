package com.bqsummer.configuration;

import com.bqsummer.service.storage.FileStorageService;
import com.bqsummer.service.storage.StorageException;
import com.bqsummer.service.storage.impl.LocalFileStorageService;
import com.bqsummer.service.storage.impl.OssFileStorageService;
import com.bqsummer.service.storage.impl.S3FileStorageService;
import com.bqsummer.service.storage.impl.HybridLocalOssFileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FileStorageConfiguration {

    @Bean
    @ConditionalOnMissingBean(FileStorageService.class)
    public FileStorageService fileStorageService(StorageProperties properties) {
        String type = properties.getType();
        if (type == null) type = "local";
        switch (type.toLowerCase()) {
            case "local":
                return new LocalFileStorageService(properties);
            case "s3":
                return new S3FileStorageService(properties);
            case "oss":
                return new OssFileStorageService(properties);
            case "hybrid":
            case "local+oss":
                return new HybridLocalOssFileStorageService(properties);
            default:
                throw new StorageException("Unsupported file.storage.type: " + type);
        }
    }
}
