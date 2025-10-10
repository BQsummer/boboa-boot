package com.bqsummer.service.storage.impl;

import com.bqsummer.configuration.StorageProperties;
import com.bqsummer.service.storage.FileStorageService;
import com.bqsummer.service.storage.StorageException;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class S3FileStorageService implements FileStorageService {
    private final S3Client s3;
    private final S3Presigner presigner; // can be null if cannot build
    private final String bucket;
    private final String basePath;
    private final String region;
    private final String publicDomain;

    public S3FileStorageService(StorageProperties props) {
        StorageProperties.S3 cfg = props.getS3();
        this.bucket = Objects.requireNonNull(cfg.getBucket(), "s3 bucket required");
        this.region = Objects.requireNonNull(cfg.getRegion(), "s3 region required");
        this.basePath = normalize(props.getBasePath());
        this.publicDomain = firstNonBlank(cfg.getPublicDomain(), props.getPublicDomain());
        AwsCredentialsProvider credProvider;
        if (notBlank(cfg.getAccessKey()) && notBlank(cfg.getSecretKey())) {
            credProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(cfg.getAccessKey(), cfg.getSecretKey()));
        } else {
            credProvider = DefaultCredentialsProvider.create();
        }
    S3ClientBuilder builder = S3Client.builder().region(Region.of(region)).credentialsProvider(credProvider).httpClient(UrlConnectionHttpClient.create());
        S3Presigner.Builder presignerBuilder = S3Presigner.builder().region(Region.of(region)).credentialsProvider(credProvider);
        if (notBlank(cfg.getEndpointOverride())) {
            URI endpoint = URI.create(cfg.getEndpointOverride());
            builder = builder.endpointOverride(endpoint);
            presignerBuilder = presignerBuilder.endpointOverride(endpoint);
        }
        this.s3 = builder.build();
        S3Presigner p;
        try { p = presignerBuilder.build(); } catch (Exception e) { p = null; }
        this.presigner = p;
        log.info("S3FileStorageService initialized bucket={} basePath={}", bucket, basePath);
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

    // New: build key under category prefix
    private String buildKeyWithCategory(String category, String filename) {
        String c = category == null ? "" : category.replace('\\','/');
        while (c.startsWith("/")) c = c.substring(1);
        while (c.endsWith("/")) c = c.substring(0, c.length()-1);
        if (c.isBlank()) return buildKey(filename);
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String ext = null;
        if (filename != null) {
            int i = filename.lastIndexOf('.');
            if (i > -1 && i < filename.length()-1) ext = filename.substring(i+1).replaceAll("[^A-Za-z0-9]","_");
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return (basePath.isEmpty()?"":basePath+"/") + c + "/" + datePart + "/" + uuid + (ext!=null?"."+ext:"");
    }

    @Override
    public String store(InputStream in, String filename, String contentType, long contentLength, Map<String, String> metadata) {
        String key = buildKey(filename);
        try {
            PutObjectRequest.Builder req = PutObjectRequest.builder().bucket(bucket).key(key);
            if (notBlank(contentType)) req = req.contentType(contentType);
            if (metadata != null && !metadata.isEmpty()) req = req.metadata(metadata);
            if (contentLength >= 0) {
                s3.putObject(req.build(), RequestBody.fromInputStream(in, contentLength));
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                in.transferTo(baos);
                byte[] bytes = baos.toByteArray();
                s3.putObject(req.build(), RequestBody.fromInputStream(new ByteArrayInputStream(bytes), bytes.length));
            }
            return key;
        } catch (Exception e) {
            throw new StorageException("S3 store failed", e);
        }
    }

    // New: categorized store under given category (e.g., "voice")
    @Override
    public String storeUnder(String category, InputStream in, String filename, String contentType, long contentLength, Map<String, String> metadata) {
        String key = buildKeyWithCategory(category, filename);
        try {
            PutObjectRequest.Builder req = PutObjectRequest.builder().bucket(bucket).key(key);
            if (notBlank(contentType)) req = req.contentType(contentType);
            if (metadata != null && !metadata.isEmpty()) req = req.metadata(metadata);
            if (contentLength >= 0) {
                s3.putObject(req.build(), RequestBody.fromInputStream(in, contentLength));
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                in.transferTo(baos);
                byte[] bytes = baos.toByteArray();
                s3.putObject(req.build(), RequestBody.fromInputStream(new ByteArrayInputStream(bytes), bytes.length));
            }
            return key;
        } catch (Exception e) {
            throw new StorageException("S3 categorized store failed", e);
        }
    }

    @Override
    public byte[] load(String key) {
        try (ResponseInputStream<GetObjectResponse> resp = s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())) {
            return resp.readAllBytes();
        } catch (Exception e) {
            throw new StorageException("S3 load failed: "+key, e);
        }
    }

    @Override
    public InputStream loadStream(String key) {
        try {
            return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception e) {
            throw new StorageException("S3 load stream failed: "+key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (S3Exception e) {
            // Treat any S3 exception as not-exists for headObject; adjust if you want to differentiate
            return false;
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception e) {
            throw new StorageException("S3 delete failed: "+key, e);
        }
    }

    @Override
    public void copy(String sourceKey, String targetKey, boolean overwrite) {
        if (!overwrite && exists(targetKey)) {
            throw new StorageException("Target exists: "+targetKey);
        }
        try {
            s3.copyObject(CopyObjectRequest.builder().sourceBucket(bucket).sourceKey(sourceKey).destinationBucket(bucket).destinationKey(targetKey).build());
        } catch (Exception e) {
            throw new StorageException("S3 copy failed", e);
        }
    }

    @Override
    public List<String> list(String prefix, int maxKeys) {
        if (maxKeys <= 0) maxKeys = 100;
        String pre = prefix == null?"":prefix;
        ListObjectsV2Request req = ListObjectsV2Request.builder().bucket(bucket).prefix(pre).maxKeys(maxKeys).build();
        try {
            ListObjectsV2Response resp = s3.listObjectsV2(req);
            List<String> r = new ArrayList<>();
            resp.contents().forEach(o -> r.add(o.key()));
            return r;
        } catch (Exception e) {
            throw new StorageException("S3 list failed", e);
        }
    }

    @Override
    public long size(String key) {
        try {
            HeadObjectResponse head = s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return head.contentLength();
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public URL generatePresignedUrl(String key, Duration expiry) {
        if (presigner == null) return null;
        try {
            GetObjectRequest getReq = GetObjectRequest.builder().bucket(bucket).key(key).build();
            GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder().signatureDuration(expiry).getObjectRequest(getReq).build();
            return presigner.presignGetObject(presignReq).url();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public URL publicUrl(String key) {
        String base;
        if (notBlank(publicDomain)) {
            base = publicDomain.endsWith("/")? publicDomain.substring(0, publicDomain.length()-1): publicDomain;
            try { return new URL(base + "/" + key); } catch (MalformedURLException e) { return null; }
        }
        // default AWS pattern
        try {
            return new URL("https://" + bucket + ".s3." + region + ".amazonaws.com/" + key);
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
