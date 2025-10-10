package com.bqsummer.common.vo.req.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SubmitFeedbackRequest {

    @NotBlank(message = "反馈类型不能为空")
    private String type;           // bug|suggestion|content|ux|other

    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 2000, message = "反馈内容不能超过2000字符")
    private String content;        // 详细的问题描述

    @Size(max = 100, message = "联系方式不能超过100字符")
    private String contact;        // 联系方式(可选)

    private List<String> images;   // 图片URL列表

    @Size(max = 50, message = "应用版本不能超过50字符")
    private String appVersion;     // 应用版本

    @Size(max = 50, message = "系统版本不能超过50字符")
    private String osVersion;      // 系统版本

    @Size(max = 100, message = "设备型号不能超过100字符")
    private String deviceModel;    // 设备型号

    private String networkType;    // wifi|4g|5g

    @Size(max = 200, message = "页面路由不能超过200字符")
    private String pageRoute;      // 发生页面路由

    private Long userId;           // 用户ID(如果登录)

    private Map<String, Object> extraData;  // 扩展信息
}
