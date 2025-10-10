package com.bqsummer.service.storage.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.OSSException;
import com.bqsummer.configuration.StorageProperties;
import com.bqsummer.service.storage.FileStorageService;
import com.bqsummer.service.storage.StorageException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Composite storage service that writes to both local filesystem and Aliyun OSS.
 * Read path prefers local as a cache; if missing, fetch from OSS and backfill to local.
 */
@Slf4j
public class HybridLocalOssFileStorageService implements FileStorageService {

    private final Path rootDir; // absolute local root
    private final String basePath; // logical prefix for both local and OSS
    private final String publicDomain;

    private final OSS oss;
    private final String bucket;

    public HybridLocalOssFileStorageService(StorageProperties props) {
        // Local setup
        this.basePath = normalize(props.getBasePath());
        StorageProperties.Local local = props.getLocal();
        Path configured = Paths.get(local.getBaseDir());
        this.rootDir = configured.isAbsolute() ? configured : Paths.get(System.getProperty("user.dir")).resolve(configured).normalize();
        if (local.isCreateDirIfMissing()) {
            try { Files.createDirectories(rootDir.resolve(basePath)); } catch (IOException e) { throw new StorageException("Cannot create local storage directory", e);} }

        // OSS setup
        StorageProperties.Oss cfg = props.getOss();
        String endpoint = Objects.requireNonNull(cfg.getEndpoint(), "oss endpoint required");
        this.bucket = Objects.requireNonNull(cfg.getBucket(), "oss bucket required");
        if (!notBlank(cfg.getAccessKey()) || !notBlank(cfg.getSecretKey())) {
            throw new StorageException("OSS accessKey/secretKey required");
        }
        this.oss = new OSSClientBuilder().build(endpoint, cfg.getAccessKey(), cfg.getSecretKey());
        this.publicDomain = firstNonBlank(cfg.getPublicDomain(), props.getPublicDomain());

        log.info("HybridLocalOssFileStorageService initialized localRoot={} bucket={} basePath={}", rootDir, bucket, basePath);
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

    private String buildKey(String originalFilename) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        String ext = null;
        if (originalFilename != null) {
            int i = originalFilename.lastIndexOf('.') ;
            if (i > -1 && i < originalFilename.length()-1) {
                ext = originalFilename.substring(i+1).replaceAll("[^A-Za-z0-9]","_");
            }
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return (basePath.isEmpty()?"":basePath+"/") + datePart + "/" + uuid + (ext!=null?"."+ext:"");
    }

    // New: build key with category prefix under basePath
    private String buildKeyWithCategory(String category, String originalFilename) {
        String safeCategory = normalize(category);
        if (safeCategory.isBlank()) {
            return buildKey(originalFilename);
        }
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String ext = null;
        if (originalFilename != null) {
            int i = originalFilename.lastIndexOf('.');
            if (i > -1 && i < originalFilename.length()-1) {
                ext = originalFilename.substring(i+1).replaceAll("[^A-Za-z0-9]","_");
            }
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String prefix = (basePath.isEmpty()?"":basePath+"/") + safeCategory + "/" + datePart + "/";
        return prefix + uuid + (ext!=null?"."+ext:"");
    }

    private Path keyToPath(String key) {
        key = key.replace('\\','/');
        while (key.startsWith("/")) key = key.substring(1);
        Path p = rootDir.resolve(key).normalize();
        if (!p.startsWith(rootDir)) throw new StorageException("Illegal key outside root: " + key);
        return p;
    }

    @Override
    public String store(InputStream in, String filename, String contentType, long contentLength, Map<String, String> metadata) {
        String key = buildKey(filename);
        Path localPath = keyToPath(key);

        // Stage to temp file to enable dual-write without buffering entire content in memory
        Path tmp = null;
        try {
            Files.createDirectories(localPath.getParent());
            tmp = Files.createTempFile(localPath.getParent(), ".staging-", ".tmp");
            try (OutputStream os = Files.newOutputStream(tmp, StandardOpenOption.TRUNCATE_EXISTING)) {
                in.transferTo(os);
            }

            // Write to local final location
            Files.move(tmp, localPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            tmp = null; // moved

            // Upload to OSS using the same key
            ObjectMetadata md = new ObjectMetadata();
            if (contentLength >= 0) md.setContentLength(contentLength); // may be inaccurate after staging; OSS doesn't require exact if we stream
            if (notBlank(contentType)) md.setContentType(contentType);
            if (metadata != null && !metadata.isEmpty()) metadata.forEach(md::addUserMetadata);
            try (InputStream ossIn = Files.newInputStream(localPath, StandardOpenOption.READ)) {
                md.setContentLength(Files.size(localPath));
                oss.putObject(bucket, key, ossIn, md);
            }
            return key;
        } catch (Exception e) {
            // rollback local if needed
            try { Files.deleteIfExists(localPath); } catch (IOException ignore) {}
            if (tmp != null) try { Files.deleteIfExists(tmp); } catch (IOException ignore) {}
            throw new StorageException("Hybrid store failed", e);
        }
    }

    // New: categorized store under given category (e.g., "voice")
    @Override
    public String storeUnder(String category, InputStream in, String filename, String contentType, long contentLength, Map<String, String> metadata) {
        String key = buildKeyWithCategory(category, filename);
        Path localPath = keyToPath(key);
        Path tmp = null;
        try {
            Files.createDirectories(localPath.getParent());
            tmp = Files.createTempFile(localPath.getParent(), ".staging-", ".tmp");
            try (OutputStream os = Files.newOutputStream(tmp, StandardOpenOption.TRUNCATE_EXISTING)) {
                in.transferTo(os);
            }
            Files.move(tmp, localPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            tmp = null;

            ObjectMetadata md = new ObjectMetadata();
            if (contentLength >= 0) md.setContentLength(contentLength);
            if (notBlank(contentType)) md.setContentType(contentType);
            if (metadata != null && !metadata.isEmpty()) metadata.forEach(md::addUserMetadata);
            try (InputStream ossIn = Files.newInputStream(localPath, StandardOpenOption.READ)) {
                md.setContentLength(Files.size(localPath));
                oss.putObject(bucket, key, ossIn, md);
            }
            return key;
        } catch (Exception e) {
            try { Files.deleteIfExists(localPath); } catch (IOException ignore) {}
            if (tmp != null) try { Files.deleteIfExists(tmp); } catch (IOException ignore) {}
            throw new StorageException("Hybrid categorized store failed", e);
        }
    }

    // Compatibility alias
    @Override
    public String storeWithPrefix(String category, java.io.InputStream in, String filename, String contentType, long contentLength, java.util.Map<String, String> metadata) {
        return storeUnder(category, in, filename, contentType, contentLength, metadata);
    }

    @Override
    public byte[] load(String key) {
        Path local = keyToPath(key);
        try {
            if (Files.exists(local)) {
                return Files.readAllBytes(local);
            }
            // fetch from OSS and backfill
            try (InputStream remoteIn = oss.getObject(bucket, key).getObjectContent()) {
                byte[] data = remoteIn.readAllBytes();
                // backfill cache best-effort
                try {
                    Files.createDirectories(local.getParent());
                    Files.write(local, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                } catch (IOException ex) {
                    log.warn("Failed to backfill local cache for key {}: {}", key, ex.getMessage());
                }
                return data;
            }
        } catch (Exception e) {
            throw new StorageException("Hybrid load failed: "+key, e);
        }
    }

    @Override
    public InputStream loadStream(String key) {
        Path local = keyToPath(key);
        try {
            if (Files.exists(local)) {
                return Files.newInputStream(local, StandardOpenOption.READ);
            }
            // download to local then stream
            Files.createDirectories(local.getParent());
            Path tmp = Files.createTempFile(local.getParent(), ".staging-", ".dl");
            try (InputStream remoteIn = oss.getObject(bucket, key).getObjectContent();
                 OutputStream os = Files.newOutputStream(tmp, StandardOpenOption.TRUNCATE_EXISTING)) {
                remoteIn.transferTo(os);
            }
            Files.move(tmp, local, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return Files.newInputStream(local, StandardOpenOption.READ);
        } catch (Exception e) {
            throw new StorageException("Hybrid load stream failed: "+key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        Path local = keyToPath(key);
        if (Files.exists(local)) return true;
        try { return oss.doesObjectExist(bucket, key); } catch (Exception e) { return false; }
    }

    @Override
    public void delete(String key) {
        Path local = keyToPath(key);
        try { Files.deleteIfExists(local); } catch (IOException e) { throw new StorageException("Local delete failed: "+key, e);}
        try { oss.deleteObject(bucket, key); } catch (OSSException e) {
            // ignore not found
            if (!(e.getErrorCode()!=null && ("NoSuchKey".equals(e.getErrorCode()) || e.getErrorCode().contains("NotFound")))) {
                throw new StorageException("OSS delete failed: "+key, e);
            }
        } catch (Exception e) {
            throw new StorageException("OSS delete failed: "+key, e);
        }
    }

    @Override
    public void copy(String sourceKey, String targetKey, boolean overwrite) {
        Path src = keyToPath(sourceKey);
        Path dst = keyToPath(targetKey);
        try {
            if (Files.exists(src)) {
                Files.createDirectories(dst.getParent());
                if (overwrite) {
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                } else {
                    Files.copy(src, dst, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        } catch (FileAlreadyExistsException e) {
            if (!overwrite) throw new StorageException("Target already exists: "+targetKey, e);
        } catch (IOException e) {
            throw new StorageException("Local copy failed", e);
        }
        // Remote copy
        if (!overwrite && exists(targetKey)) {
            throw new StorageException("Target exists: "+targetKey);
        }
        try {
            oss.copyObject(bucket, sourceKey, bucket, targetKey);
        } catch (Exception e) {
            throw new StorageException("OSS copy failed", e);
        }
    }

    @Override
    public List<String> list(String prefix, int maxKeys) {
        final int limit = (maxKeys <= 0) ? 100 : maxKeys;
        final String pre = (prefix == null) ? "" : prefix;
        try {
            ListObjectsRequest req = new ListObjectsRequest(bucket);
            req.setPrefix(pre);
            req.setMaxKeys(limit);
            ObjectListing listing = oss.listObjects(req);
            List<String> keys = new ArrayList<>();
            listing.getObjectSummaries().forEach(s -> keys.add(s.getKey()));
            return keys;
        } catch (Exception e) {
            // fallback to local listing
            List<String> results = new ArrayList<>();
            Path base = rootDir.resolve(basePath);
            Path start = (pre.isBlank()) ? base : rootDir.resolve(pre);
            if (!Files.exists(start)) return results;
            try (java.util.stream.Stream<Path> stream = Files.walk(start, 3)) {
                stream.filter(Files::isRegularFile).forEach(p -> {
                    if (results.size() >= limit) return;
                    String rel = rootDir.relativize(p).toString().replace('\\','/');
                    results.add(rel);
                });
            } catch (IOException ex) {
                log.warn("Local list fallback failed: {}", ex.getMessage());
            }
            return results;
        }
    }

    @Override
    public long size(String key) {
        try { return Files.size(keyToPath(key)); } catch (IOException ignore) {}
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
        return generatePresignedUrl(key, Duration.ofMinutes(10));
    }
}
