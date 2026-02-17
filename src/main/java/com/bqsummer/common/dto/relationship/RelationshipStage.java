package com.bqsummer.common.dto.relationship;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("relationship_stages")
public class RelationshipStage {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String code;

    private String name;

    private Integer level;

    private String description;

    @TableField("is_active")
    private Boolean isActive;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
