package com.bqsummer.common.dto.im;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("voice_assets")
public class VoiceAsset {
    @TableId
    private Long id;
    private Long userId; // 所属用户（上传/生成者）
    private Long messageId; // 可选：关联的消息ID

    private String fileKey; // 存储键
    private String contentType; // MIME 类型，例如 audio/mpeg
    private Long sizeBytes; // 文件大小
    private Integer durationMs; // 时长（毫秒），可选
    private String format; // 例如 mp3/wav/ogg，可选

    private Integer isDeleted = 0;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
