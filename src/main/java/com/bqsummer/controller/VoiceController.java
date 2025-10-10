package com.bqsummer.controller;

import com.bqsummer.common.dto.im.VoiceAsset;
import com.bqsummer.common.vo.req.im.VoiceGenerateRequest;
import com.bqsummer.common.vo.resp.im.VoiceAssetResp;
import com.bqsummer.service.im.VoiceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/voice")
public class VoiceController {

    @Autowired
    private VoiceService voiceService;

    private Long currentUserId() {
        Object d = SecurityContextHolder.getContext().getAuthentication().getDetails();
        return (d instanceof Long) ? (Long) d : null;
    }

    private VoiceAssetResp toResp(VoiceAsset v) {
        VoiceAssetResp r = new VoiceAssetResp();
        r.setId(v.getId());
        r.setUserId(v.getUserId());
        r.setMessageId(v.getMessageId());
        r.setFileKey(v.getFileKey());
        r.setContentType(v.getContentType());
        r.setSizeBytes(v.getSizeBytes());
        r.setDurationMs(v.getDurationMs());
        r.setFormat(v.getFormat());
        r.setCreatedAt(v.getCreatedAt());
        URL url = voiceService.publicUrl(v.getFileKey());
        r.setUrl(url);
        return r;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VoiceAssetResp upload(@RequestPart("file") MultipartFile file,
                                 @RequestParam(value = "durationMs", required = false) Integer durationMs,
                                 @RequestParam(value = "messageId", required = false) Long messageId) throws Exception {
        Long uid = currentUserId();
        VoiceAsset v = voiceService.upload(uid, file, durationMs, messageId);
        return toResp(v);
    }

    @PostMapping("/generate")
    public VoiceAssetResp generate(@Valid @RequestBody VoiceGenerateRequest req) {
        Long uid = req.getUserId() != null ? req.getUserId() : currentUserId();
        VoiceAsset v = voiceService.saveGenerated(uid, req.getFilename(), req.getContentType(), req.getBase64Data(), req.getDurationMs(), req.getMessageId());
        return toResp(v);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<VoiceAssetResp> get(@PathVariable Long id) {
        VoiceAsset v = voiceService.get(id);
        if (v == null) return ResponseEntity.notFound().build();
        if (!v.getUserId().equals(currentUserId())) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(toResp(v));
    }

    @GetMapping("/list")
    public List<VoiceAssetResp> list(@RequestParam(defaultValue = "20") int limit,
                                     @RequestParam(required = false) Long beforeId) {
        Long uid = currentUserId();
        return voiceService.listByUser(uid, Math.min(Math.max(1, limit), 100), beforeId).stream().map(this::toResp).collect(Collectors.toList());
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<InputStreamResource> stream(@PathVariable Long id) {
        VoiceAsset v = voiceService.get(id);
        if (v == null) return ResponseEntity.notFound().build();
        if (!v.getUserId().equals(currentUserId())) return ResponseEntity.status(403).build();
        InputStreamResource resource = new InputStreamResource(voiceService.openStream(v.getFileKey()));
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok().contentType(MediaType.parseMediaType(v.getContentType()));
        long size = v.getSizeBytes() == null ? -1L : v.getSizeBytes();
        if (size >= 0) builder.contentLength(size);
        String filename = (v.getFormat() != null ? ("voice." + v.getFormat()) : "voice");
        builder.header("Content-Disposition", "inline; filename=\"" + filename + "\"");
        return builder.body(resource);
    }

    @GetMapping("/{id}/url")
    public ResponseEntity<String> url(@PathVariable Long id) {
        VoiceAsset v = voiceService.get(id);
        if (v == null) return ResponseEntity.notFound().build();
        if (!v.getUserId().equals(currentUserId())) return ResponseEntity.status(403).build();
        URL url = voiceService.publicUrl(v.getFileKey());
        if (url == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(url.toString());
    }
}
