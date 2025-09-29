package com.bqsummer.plugin.points.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_account")
public class PointsAccount {
    private Long id;
    private Long userId;
    private Long balance; // current available points
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}

