package com.bqsummer.service.storage.impl;

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
import java.util.stream.Stream;

@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private final Path rootDir; // absolute
    private final String basePath; // logical prefix (no leading slash)
    private final String publicDomain;

    public LocalFileStorageService(StorageProperties props) {
        this.basePath = normalize(props.getBasePath());
        StorageProperties.Local local = props.getLocal();
        Path configured = Paths.get(local.getBaseDir());
        this.rootDir = configured.isAbsolute() ? configured : Paths.get(System.getProperty("user.dir")).resolve(configured).normalize();
        this.publicDomain = props.getPublicDomain();
        if (local.isCreateDirIfMissing()) {
            try {
                Files.createDirectories(rootDir.resolve(basePath));
            } catch (IOException e) {
                throw new StorageException("Cannot create local storage directory", e);
            }
        }
        log.info("LocalFileStorageService initialized at {} basePath {}", rootDir, basePath);
    }

    private String normalize(String s) {
        if (s == null || s.isBlank()) return "";
        s = s.replace('\\', '/');
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

    private String buildKeyWithCategory(String category, String originalFilename) {
        String safeCategory = normalize(category);
        if (safeCategory.isBlank()) return buildKey(originalFilename);
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String ext = null;
        if (originalFilename != null) {
            int i = originalFilename.lastIndexOf('.') ;
            if (i > -1 && i < originalFilename.length()-1) {
                ext = originalFilename.substring(i+1).replaceAll("[^A-Za-z0-9]","_");
            }
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return (basePath.isEmpty()?"":basePath+"/") + safeCategory + "/" + datePart + "/" + uuid + (ext!=null?"."+ext:"");
    }

    private Path keyToPath(String key) {
        key = key.replace('\\','/');
        while (key.startsWith("/")) key = key.substring(1);
        Path p = rootDir.resolve(key).normalize();
        // Prevent path traversal outside rootDir
        if (!p.startsWith(rootDir)) {
            throw new StorageException("Illegal key outside root: " + key);
        }
        return p;
    }

    @Override
    public String store(InputStream in, String filename, String contentType, long contentLength, Map<String, String> metadata) {
        String key = buildKey(filename);
        Path path = keyToPath(key);
        try {
            Files.createDirectories(path.getParent());
            // For local storage we can stream directly regardless of contentLength
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException e) {
            throw new StorageException("Failed to store file", e);
        }
    }

    // New: categorized store under given category (e.g., "voice")
    public String storeUnder(String category, InputStream in, String filename, String contentType, long contentLength, Map<String, String> metadata) {
        String key = buildKeyWithCategory(category, filename);
        Path path = keyToPath(key);
        try {
            Files.createDirectories(path.getParent());
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException e) {
            throw new StorageException("Failed to store file (categorized)", e);
        }
    }

    // Compatibility alias
    @Override
    public String storeWithPrefix(String category, InputStream in, String filename, String contentType, long contentLength, Map<String, String> metadata) {
        return storeUnder(category, in, filename, contentType, contentLength, metadata);
    }

    @Override
    public byte[] load(String key) {
        Path p = keyToPath(key);
        try {
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw new StorageException("Failed to read file: " + key, e);
        }
    }

    @Override
    public InputStream loadStream(String key) {
        Path p = keyToPath(key);
        try {
            return Files.newInputStream(p, StandardOpenOption.READ);
        } catch (IOException e) {
            throw new StorageException("Failed to open stream: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(keyToPath(key));
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(keyToPath(key));
        } catch (IOException e) {
            throw new StorageException("Failed to delete: " + key, e);
        }
    }

    @Override
    public void copy(String sourceKey, String targetKey, boolean overwrite) {
        Path src = keyToPath(sourceKey);
        Path dst = keyToPath(targetKey);
        try {
            Files.createDirectories(dst.getParent());
            if (overwrite) {
                Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            } else {
                Files.copy(src, dst, StandardCopyOption.COPY_ATTRIBUTES);
            }
        } catch (FileAlreadyExistsException e) {
            if (!overwrite) {
                throw new StorageException("Target already exists: " + targetKey, e);
            }
        } catch (IOException e) {
            throw new StorageException("Copy failed", e);
        }
    }

    @Override
    public List<String> list(String prefix, int maxKeys) {
        final int limit = maxKeys <= 0 ? 100 : maxKeys;
        String norm = normalize(prefix);
        // Allow callers to pass prefix with or without basePath
        if (!basePath.isEmpty()) {
            if (norm.equals(basePath)) {
                norm = ""; // root inside basePath
            } else if (norm.startsWith(basePath + "/")) {
                norm = norm.substring(basePath.length()+1);
            }
        }
        Path base = rootDir.resolve(basePath);
        Path start = norm.isEmpty()? base : base.resolve(norm);
        List<String> results = new ArrayList<>();
        if (!Files.exists(start)) return results;
        try (Stream<Path> stream = Files.walk(start, 3)) { // limit depth 3 for perf
            stream.filter(Files::isRegularFile).forEach(p -> {
                if (results.size() >= limit) return;
                String rel = base.relativize(p).toString().replace('\\','/');
                results.add((basePath.isEmpty()?"":basePath+"/") + rel);
            });
        } catch (IOException e) {
            throw new StorageException("List failed", e);
        }
        return results;
    }

    @Override
    public long size(String key) {
        try {
            return Files.size(keyToPath(key));
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public URL generatePresignedUrl(String key, Duration expiry) {
        return null; // not supported for plain local storage
    }

    @Override
    public URL publicUrl(String key) {
        if (publicDomain == null || publicDomain.isBlank()) return null;
        StringBuilder sb = new StringBuilder();
        sb.append(publicDomain);
        if (!publicDomain.endsWith("/")) sb.append('/');
        sb.append(key);
        try {
            return new URL(sb.toString());
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
