package com.bqsummer.common.dto.feedback;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("feedback")
public class Feedback {
    private Long id;
    private String type;           // bug|suggestion|content|ux|other
    private String content;        // 详细的问题描述
    private String contact;        // 联系方式(可选)
    private String images;         // JSON数组字符串
    private String appVersion;     // 应用版本
    private String osVersion;      // 系统版本
    private String deviceModel;    // 设备型号
    private String networkType;    // wifi|4g|5g
    private String pageRoute;      // 发生页面路由
    private Long userId;           // 用户ID(如果登录)
    private String extraData;      // 扩展信息(JSON字符串)

    private String status;         // NEW|IN_PROGRESS|RESOLVED|REJECTED
    private Long handlerUserId;    // 处理人(管理员)
    private String handlerRemark;  // 处理备注

    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}

