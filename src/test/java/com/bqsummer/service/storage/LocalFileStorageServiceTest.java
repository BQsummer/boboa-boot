package com.bqsummer.service.storage;

import com.bqsummer.configuration.StorageProperties;
import com.bqsummer.service.storage.impl.LocalFileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LocalFileStorageServiceTest {

    private LocalFileStorageService service;
    private Path baseDir;

    @BeforeEach
    void setUp() throws IOException {
        StorageProperties props = new StorageProperties();
        props.setType("local");
        props.setBasePath("ut");
        props.getLocal().setBaseDir("target/test-files");
        props.getLocal().setCreateDirIfMissing(true);
        service = new LocalFileStorageService(props);
        baseDir = Paths.get(System.getProperty("user.dir"))
                .resolve(props.getLocal().getBaseDir())
                .resolve(props.getBasePath());
    }

    @AfterEach
    void cleanup() throws IOException {
        if (Files.exists(baseDir)) {
            try (var s = Files.walk(baseDir)) {
                s.sorted((a,b)->b.compareTo(a)).forEach(p -> {
                    try { Files.deleteIfExists(p);} catch (IOException ignored) {}
                });
            }
        }
    }

    @Test
    void testStoreAndLoad() {
        byte[] data = ("hello-" + UUID.randomUUID()).getBytes();
        String key = service.store(new ByteArrayInputStream(data), "test.txt", "text/plain", data.length, Map.of("k","v"));
        assertNotNull(key);
        assertTrue(service.exists(key));
        byte[] loaded = service.load(key);
        assertArrayEquals(data, loaded);
        assertTrue(service.size(key) > 0);
        List<String> listed = service.list("ut", 10);
        assertFalse(listed.isEmpty());
    }

    @Test
    void testCopyAndDelete() {
        byte[] data = "abc".getBytes();
        String key = service.store(new ByteArrayInputStream(data), "copy.txt", "text/plain", data.length, null);
        String copyKey = key + ".bak";
        service.copy(key, copyKey, true);
        assertTrue(service.exists(copyKey));
        service.delete(key);
        assertFalse(service.exists(key));
        service.delete(copyKey);
        assertFalse(service.exists(copyKey));
    }

    @Test
    void testRejectPathTraversal() {
        assertThrows(StorageException.class, () -> service.load("../etc/passwd"));
        assertThrows(StorageException.class, () -> service.load("..\\etc\\passwd"));
        assertThrows(StorageException.class, () -> service.delete("../../secret"));
    }
}
