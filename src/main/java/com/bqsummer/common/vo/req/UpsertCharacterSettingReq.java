package com.bqsummer.common.vo.req;

import lombok.Data;

@Data
public class UpsertCharacterSettingReq {
    private String name;         // 自定义人名
    private String avatarUrl;    // 自定义头像
    private String memorialDay;  // 纪念日(yyyy-MM-dd)
    private String relationship; // 关系
    private String background;   // 背景
    private String language;     // 语言
    private String customParams; // 其他参数(JSON字符串)
}

