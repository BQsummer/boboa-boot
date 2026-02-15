package com.bqsummer.service;

import com.bqsummer.configuration.StorageProperties;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemFileService {

    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;

    public String upload(Long userId, MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new SnorlaxClientException(400, "file is required");
        }
        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "unknown";
        String safeCategory = normalizeCategory(category);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("uploaderId", String.valueOf(userId));
        metadata.put("category", safeCategory);

        final String fileKey;
        try {
            fileKey = fileStorageService.storeUnder(
                    safeCategory,
                    file.getInputStream(),
                    originalName,
                    file.getContentType(),
                    file.getSize(),
                    metadata
            );
        } catch (IOException e) {
            throw new SnorlaxClientException(400, "upload failed");
        }
        return fileKey;
    }

    public Map<String, Object> list(int pageNum, int pageSize, String keyword, String category) {
        int safePageNum = Math.max(1, pageNum);
        int safePageSize = Math.min(Math.max(1, pageSize), 100);
        int fetchSize = Math.min(Math.max(safePageNum * safePageSize * 3, 200), 2000);
        String prefix = buildPrefix(category);
        List<String> keys = new ArrayList<>(fileStorageService.list(prefix, fetchSize));
        Collections.sort(keys, Collections.reverseOrder());

        String safeKeyword = trimToNull(keyword);
        if (safeKeyword != null) {
            keys = keys.stream().filter(key -> key.contains(safeKeyword)).collect(Collectors.toList());
        }

        int total = keys.size();
        int fromIndex = Math.min((safePageNum - 1) * safePageSize, total);
        int toIndex = Math.min(fromIndex + safePageSize, total);
        List<String> paged = keys.subList(fromIndex, toIndex);

        Map<String, Object> result = new HashMap<>();
        result.put("keys", paged);
        result.put("total", total);
        result.put("page", safePageNum);
        result.put("pageSize", safePageSize);
        result.put("totalPages", Math.max((int) Math.ceil(total * 1.0 / safePageSize), 1));
        return result;
    }

    public String rename(String key, String fileName, String category) {
        if (!StringUtils.hasText(key)) {
            throw new SnorlaxClientException(400, "key is required");
        }
        String sourceKey = key.trim();
        if (!fileStorageService.exists(sourceKey)) {
            throw new SnorlaxClientException(404, "file not found");
        }

        String targetFileName = sanitizeFilename(StringUtils.hasText(fileName) ? fileName.trim() : extractFileName(sourceKey));
        if (!StringUtils.hasText(targetFileName)) {
            throw new SnorlaxClientException(400, "fileName is invalid");
        }

        String targetKey;
        if (StringUtils.hasText(category)) {
            String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String categoryPrefix = buildPrefix(category.trim());
            targetKey = appendSegment(appendSegment(categoryPrefix, date), targetFileName);
        } else {
            int idx = sourceKey.lastIndexOf('/');
            String parent = idx > 0 ? sourceKey.substring(0, idx) : "";
            targetKey = appendSegment(parent, targetFileName);
        }

        if (targetKey.equals(sourceKey)) {
            return sourceKey;
        }

        if (fileStorageService.exists(targetKey)) {
            String randomPrefix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            int idx = targetKey.lastIndexOf('/');
            String parent = idx > 0 ? targetKey.substring(0, idx) : "";
            targetKey = appendSegment(parent, randomPrefix + "_" + targetFileName);
        }

        fileStorageService.copy(sourceKey, targetKey, false);
        fileStorageService.delete(sourceKey);
        return targetKey;
    }

    public void delete(String key) {
        if (!StringUtils.hasText(key)) {
            throw new SnorlaxClientException(400, "key is required");
        }
        fileStorageService.delete(key.trim());
    }

    public URL resolveAccessUrl(String fileKey) {
        if (!StringUtils.hasText(fileKey)) {
            return null;
        }
        URL publicUrl = fileStorageService.publicUrl(fileKey);
        if (publicUrl != null) {
            return publicUrl;
        }
        return fileStorageService.generatePresignedUrl(fileKey, Duration.ofHours(2));
    }

    public long resolveSize(String key) {
        if (!StringUtils.hasText(key)) {
            return -1L;
        }
        return fileStorageService.size(key);
    }

    public String resolveStorageType() {
        return trimToNull(storageProperties.getType()) == null ? "local" : storageProperties.getType();
    }

    public String extractCategory(String key) {
        if (!StringUtils.hasText(key)) {
            return "system";
        }
        String normalized = trimSlashes(key);
        String base = trimSlashes(storageProperties.getBasePath());
        if (StringUtils.hasText(base) && normalized.startsWith(base + "/")) {
            normalized = normalized.substring(base.length() + 1);
        }
        String[] parts = normalized.split("/");
        if (parts.length >= 3 && parts[1].matches("\\d{8}")) {
            return parts[0];
        }
        return "system";
    }

    private String normalizeCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return "system";
        }
        return category.trim();
    }

    private String buildPrefix(String category) {
        String base = trimSlashes(storageProperties.getBasePath());
        String c = trimSlashes(category);
        if (!StringUtils.hasText(c)) {
            return base;
        }
        return appendSegment(base, c);
    }

    private String extractFileName(String key) {
        int idx = key.lastIndexOf('/');
        return idx >= 0 ? key.substring(idx + 1) : key;
    }

    private String appendSegment(String prefix, String segment) {
        String p = trimSlashes(prefix);
        String s = trimSlashes(segment);
        if (!StringUtils.hasText(p)) {
            return s;
        }
        if (!StringUtils.hasText(s)) {
            return p;
        }
        return p + "/" + s;
    }

    private String trimSlashes(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String v = value.trim().replace('\\', '/');
        while (v.startsWith("/")) {
            v = v.substring(1);
        }
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    private String sanitizeFilename(String filename) {
        String safe = filename.replace('\\', '_').replace('/', '_').trim();
        if (!StringUtils.hasText(safe)) {
            return null;
        }
        return safe;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
