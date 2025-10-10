package com.bqsummer.service.im;

import com.bqsummer.common.dto.im.VoiceAsset;
import com.bqsummer.repository.VoiceAssetRepository;
import com.bqsummer.service.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VoiceService {

    private final VoiceAssetRepository repository;
    private final FileStorageService storage;

    public VoiceService(VoiceAssetRepository repository, FileStorageService storage) {
        this.repository = repository;
        this.storage = storage;
    }

    public VoiceAsset upload(Long userId, MultipartFile file, Integer durationMs, Long messageId) throws IOException {
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        long size = file.getSize();
        Map<String,String> meta = new HashMap<>();
        if (durationMs != null) meta.put("duration", String.valueOf(durationMs));
        String key = storage.storeUnder("voice", file.getInputStream(), filename, contentType, size, meta);
        return saveRecord(userId, messageId, key, contentType, size, durationMs, filename);
    }

    public VoiceAsset saveGenerated(Long userId, String filename, String contentType, String base64Data, Integer durationMs, Long messageId) {
        byte[] data = Base64.getDecoder().decode(base64Data);
        Map<String,String> meta = new HashMap<>();
        if (durationMs != null) meta.put("duration", String.valueOf(durationMs));
        String key = storage.storeUnder("voice", new ByteArrayInputStream(data), filename, contentType, data.length, meta);
        return saveRecord(userId, messageId, key, contentType, (long) data.length, durationMs, filename);
    }

    private VoiceAsset saveRecord(Long userId, Long messageId, String key, String contentType, Long size, Integer durationMs, String filename) {
        VoiceAsset v = new VoiceAsset();
        v.setUserId(userId);
        v.setMessageId(messageId);
        v.setFileKey(key);
        v.setContentType(contentType);
        v.setSizeBytes(size);
        v.setDurationMs(durationMs);
        v.setFormat(extFrom(filename));
        v.setIsDeleted(0);
        v.setCreatedAt(LocalDateTime.now());
        v.setUpdatedAt(LocalDateTime.now());
        return repository.save(v);
    }

    private String extFrom(String filename) {
        if (filename == null) return null;
        int i = filename.lastIndexOf('.');
        if (i > -1 && i < filename.length()-1) return filename.substring(i+1).toLowerCase();
        return null;
    }

    public VoiceAsset get(Long id) { return repository.findById(id); }

    public List<VoiceAsset> listByUser(Long userId, int limit, Long beforeId) { return repository.listByUser(userId, limit, beforeId); }

    public URL publicUrl(String key) { return storage.publicUrl(key); }

    public long size(String key) { return storage.size(key); }

    public InputStream openStream(String key) { return storage.loadStream(key); }
}
