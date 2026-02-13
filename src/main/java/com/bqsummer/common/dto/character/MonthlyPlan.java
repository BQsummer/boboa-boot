package com.bqsummer.common.dto.character;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 虚拟人物月度计划
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyPlan {

    private Long id;

    /**
     * 关联的虚拟人物ID
     */
    private Long characterId;

    /**
     * 日期规则，如 day=5 或 weekday=1,week=2
     */
    private String dayRule;

    /**
     * 活动开始时间
     */
    private LocalTime startTime;

    /**
     * 持续时长（分钟）
     */
    private Integer durationMin;

    /**
     * 活动地点
     */
    private String location;

    /**
     * 活动内容
     */
    private String action;

    /**
     * 参与者列表（JSON字符串）
     */
    private String participants;

    /**
     * 扩展信息（JSON字符串）
     */
    private String extra;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableLogic(value = "false", delval = "true")
    @TableField("is_deleted")
    private Boolean isDeleted;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
