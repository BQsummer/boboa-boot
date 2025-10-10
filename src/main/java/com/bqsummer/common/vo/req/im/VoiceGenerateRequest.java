package com.bqsummer.common.vo.req.im;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VoiceGenerateRequest {
    @NotNull
    private Long userId; // 允许服务端作业任务指定保存归属用户；若前台调用可忽略并从上下文取

    @NotBlank
    private String filename;

    @NotBlank
    private String contentType; // e.g. audio/mpeg

    @NotBlank
    private String base64Data; // Base64 编码的音频数据

    private Integer durationMs; // 可选

    private Long messageId; // 可选：关联消息
}

