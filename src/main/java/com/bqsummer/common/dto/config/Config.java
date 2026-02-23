package com.bqsummer.common.dto.config;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bqsummer.constant.ConfigStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("config")
@Builder
public class Config {
    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String env;
    private String application;
    private String name;
    @TableField("\"desc\"")
    private String desc;
    private String value;
    private String type;
    @TableField("\"sensitive\"")
    private String sensitive;
    @EnumValue
    private ConfigStatus status;
    private String catalog;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
