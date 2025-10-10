package com.bqsummer.service.storage;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Unified file storage abstraction shielding underlying provider (local filesystem, S3, OSS, etc).
 */
public interface FileStorageService {

    /**
     * Store a file (small or streaming) and return its storage key.
     * @param in input stream (will be fully consumed and not closed by implementation)
     * @param filename original filename (used for key derivation when key not provided)
     * @param contentType optional content type
     * @param contentLength optional length (-1 if unknown)
     * @param metadata optional user metadata map
     * @return storage key (unique path / object key)
     */
    String store(InputStream in, String filename, String contentType, long contentLength, Map<String,String> metadata);

    /** Load as bytes (for small files). */
    byte[] load(String key);

    /** Get streaming access. Caller must close. */
    InputStream loadStream(String key);

    /** Check existence. */
    boolean exists(String key);

    /** Delete a single object; ignore if absent. */
    void delete(String key);

    /** Copy object inside same backend. */
    void copy(String sourceKey, String targetKey, boolean overwrite);

    /** List keys under a prefix (not recursive semantics guaranteed). */
    List<String> list(String prefix, int maxKeys);

    /** Return length in bytes or -1 if unknown / not supported. */
    long size(String key);

    /** Generate a time limited signed access URL if supported, else return null. */
    URL generatePresignedUrl(String key, Duration expiry);

    /** Get a public URL if object is public or backend supports stable url, else null. */
    URL publicUrl(String key);

    // New: default categorized store under a logical category (e.g., "voice", "image")
    default String storeUnder(String category, InputStream in, String filename, String contentType, long contentLength, Map<String,String> metadata) {
        // By default, ignore category and delegate to store. Implementations can override to place under category-specific prefixes.
        return store(in, filename, contentType, contentLength, metadata);
    }

    // Compatibility alias for older naming
    default String storeWithPrefix(String category, InputStream in, String filename, String contentType, long contentLength, Map<String,String> metadata) {
        return storeUnder(category, in, filename, contentType, contentLength, metadata);
    }
}
