package com.bqsummer.service.storage.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
import com.bqsummer.configuration.StorageProperties;
import com.bqsummer.service.storage.FileStorageService;
import com.bqsummer.service.storage.StorageException;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class OssFileStorageService implements FileStorageService {
    private final OSS oss;
    private final String bucket;
    private final String basePath;
    private final String publicDomain;

    public OssFileStorageService(StorageProperties props) {
        StorageProperties.Oss cfg = props.getOss();
        this.bucket = Objects.requireNonNull(cfg.getBucket(), "oss bucket required");
        String endpoint = Objects.requireNonNull(cfg.getEndpoint(), "oss endpoint required");
        this.basePath = normalize(props.getBasePath());
        this.publicDomain = firstNonBlank(cfg.getPublicDomain(), props.getPublicDomain());
        if (!notBlank(cfg.getAccessKey()) || !notBlank(cfg.getSecretKey())) {
            throw new StorageException("OSS accessKey/secretKey required");
        }
        this.oss = new OSSClientBuilder().build(endpoint, cfg.getAccessKey(), cfg.getSecretKey());
        log.info("OssFileStorageService initialized bucket={} basePath={}", bucket, basePath);
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private String firstNonBlank(String a, String b) { return notBlank(a)?a:(notBlank(b)?b:null); }

    private String normalize(String s) {
        if (s == null || s.isBlank()) return "";
        s = s.replace('\\','/');
        while (s.startsWith("/")) s = s.substring(1);
        while (s.endsWith("/")) s = s.substring(0, s.length()-1);
        return s;
    }

    private String buildKey(String filename) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String ext = null;
        if (filename != null) {
            int i = filename.lastIndexOf('.');
            if (i > -1 && i < filename.length()-1) ext = filename.substring(i+1).replaceAll("[^A-Za-z0-9]","_");
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return (basePath.isEmpty()?"":basePath+"/") + datePart + "/" + uuid + (ext!=null?"."+ext:"");
    }

    private String buildKeyWithCategory(String category, String filename) {
        String safeCategory = normalize(category);
        if (safeCategory.isBlank()) return buildKey(filename);
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String ext = null;
        if (filename != null) {
            int i = filename.lastIndexOf('.');
            if (i > -1 && i < filename.length()-1) ext = filename.substring(i+1).replaceAll("[^A-Za-z0-9]","_");
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return (basePath.isEmpty()?"":basePath+"/") + safeCategory + "/" + datePart + "/" + uuid + (ext!=null?"."+ext:"");
    }

    @Override
    public String store(InputStream in, String filename, String contentType, long contentLength, Map<String, String> metadata) {
        String key = buildKey(filename);
        try {
            ObjectMetadata md = new ObjectMetadata();
            if (contentLength >= 0) md.setContentLength(contentLength);
            if (notBlank(contentType)) md.setContentType(contentType);
            if (metadata != null && !metadata.isEmpty()) metadata.forEach(md::addUserMetadata);
            if (contentLength >= 0) {
                oss.putObject(bucket, key, in, md);
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                in.transferTo(baos);
                byte[] b = baos.toByteArray();
                md.setContentLength(b.length);
                oss.putObject(bucket, key, new ByteArrayInputStream(b), md);
            }
            return key;
        } catch (Exception e) {
            throw new StorageException("OSS store failed", e);
        }
    }

    // New: categorized store under given category (e.g., "voice")
    public String storeUnder(String category, InputStream in, String filename, String contentType, long contentLength, Map<String, String> metadata) {
        String key = buildKeyWithCategory(category, filename);
        try {
            ObjectMetadata md = new ObjectMetadata();
            if (contentLength >= 0) md.setContentLength(contentLength);
            if (notBlank(contentType)) md.setContentType(contentType);
            if (metadata != null && !metadata.isEmpty()) metadata.forEach(md::addUserMetadata);
            if (contentLength >= 0) {
                oss.putObject(bucket, key, in, md);
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                in.transferTo(baos);
                byte[] b = baos.toByteArray();
                md.setContentLength(b.length);
                oss.putObject(bucket, key, new ByteArrayInputStream(b), md);
            }
            return key;
        } catch (Exception e) {
            throw new StorageException("OSS categorized store failed", e);
        }
    }

    @Override
    public byte[] load(String key) {
        try (OSSObject obj = oss.getObject(bucket, key)) {
            return obj.getObjectContent().readAllBytes();
        } catch (Exception e) {
            throw new StorageException("OSS load failed: "+key, e);
        }
    }

    @Override
    public InputStream loadStream(String key) {
        try {
            return oss.getObject(bucket, key).getObjectContent();
        } catch (Exception e) {
            throw new StorageException("OSS load stream failed: "+key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return oss.doesObjectExist(bucket, key);
        } catch (Exception e) {
            log.error("OSS exists check failed for key {}", key, e);
            return false;
        }
    }

    @Override
    public void delete(String key) {
        try { oss.deleteObject(bucket, key); } catch (Exception e) { throw new StorageException("OSS delete failed", e);} }

    @Override
    public void copy(String sourceKey, String targetKey, boolean overwrite) {
        if (!overwrite && exists(targetKey)) throw new StorageException("Target exists: "+targetKey);
        try { oss.copyObject(bucket, sourceKey, bucket, targetKey); } catch (Exception e) { throw new StorageException("OSS copy failed", e);} }

    @Override
    public List<String> list(String prefix, int maxKeys) {
        if (maxKeys <= 0) maxKeys = 100;
        String pre = prefix==null?"":prefix;
        ListObjectsRequest req = new ListObjectsRequest(bucket);
        req.setPrefix(pre);
        req.setMaxKeys(maxKeys);
        ObjectListing listing = oss.listObjects(req);
        List<String> keys = new ArrayList<>();
        listing.getObjectSummaries().forEach(s -> keys.add(s.getKey()));
        return keys;
    }

    @Override
    public long size(String key) {
        try { ObjectMetadata md = oss.getObjectMetadata(bucket, key); return md.getContentLength(); } catch (Exception e) { return -1; }
    }

    @Override
    public URL generatePresignedUrl(String key, Duration expiry) {
        try {
            Duration exp = (expiry == null || expiry.isNegative() || expiry.isZero()) ? Duration.ofMinutes(10) : expiry;
            return oss.generatePresignedUrl(bucket, key, new Date(System.currentTimeMillis()+exp.toMillis()));
        } catch (Exception e) { return null; }
    }

    @Override
    public URL publicUrl(String key) {
        if (notBlank(publicDomain)) {
            try {
                String base = publicDomain.endsWith("/")? publicDomain.substring(0, publicDomain.length()-1): publicDomain;
                return new URL(base + "/" + key);
            } catch (MalformedURLException e) { return null; }
        }
        return generatePresignedUrl(key, Duration.ofMinutes(10)); // fallback short lived
    }
}
