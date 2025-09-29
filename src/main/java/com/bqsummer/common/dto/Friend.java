package com.bqsummer.common.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friends")
public class Friend {
    private Long id;
    private Long userId;
    private Long friendUserId;
    private LocalDateTime createdTime;
}

