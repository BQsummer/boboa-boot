package com.bqsummer.common.dto.point;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_bucket")
public class PointsBucket {
    private Long id;
    private Long userId;
    private String activityCode;
    private Long amount;
    private Long remaining;
    private LocalDateTime expireAt;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}

