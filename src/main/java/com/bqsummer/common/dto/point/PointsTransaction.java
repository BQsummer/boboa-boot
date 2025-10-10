package com.bqsummer.common.dto.point;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@TableName("points_transaction")
public class PointsTransaction {
    private Long id;
    private Long userId;
    private String type; // EARN/CONSUME/EXPIRE/REFUND
    private Long amount; // positive amount
    private String activityCode;
    private String description;
    private LocalDateTime createdTime;
}


