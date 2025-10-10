package com.bqsummer.common.vo.resp.im;

import lombok.Data;

import java.net.URL;
import java.time.LocalDateTime;

@Data
public class VoiceAssetResp {
    private Long id;
    private Long userId;
    private Long messageId;
    private String fileKey;
    private String contentType;
    private Long sizeBytes;
    private Integer durationMs;
    private String format;
    private URL url; // 可公开访问URL（若可用）
    private LocalDateTime createdAt;
}

