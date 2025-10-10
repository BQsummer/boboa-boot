package com.bqsummer.common.dto.point;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_activity")
public class PointsActivity {
    private Long id;
    private String code;            // 唯一活动编码
    private String name;            // 活动名称
    private String description;     // 活动描述
    private String status;          // ENABLED/DISABLED
    private LocalDateTime startTime; // 活动开始时间
    private LocalDateTime endTime;   // 活动结束时间
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}

