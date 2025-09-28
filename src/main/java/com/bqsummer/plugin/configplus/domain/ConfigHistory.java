package com.bqsummer.plugin.configplus.domain;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("config_history")
public class ConfigHistory {
    @TableId
    private Long id;
    private String env;
    private String application;
    private String name;
    @TableField("`desc`")
    private String desc;
    private String value;
    private String type;
    @TableField("`sensitive`")
    private String sensitive;
    @EnumValue
    private ConfigStatus status;
    private String catalog;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
