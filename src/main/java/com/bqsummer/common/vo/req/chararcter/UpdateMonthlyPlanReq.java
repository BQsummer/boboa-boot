package com.bqsummer.common.vo.req.chararcter;

import lombok.Data;

/**
 * 更新月度计划请求
 */
@Data
public class UpdateMonthlyPlanReq {

    /**
     * 日期规则，如 day=5 或 weekday=1,week=2（可选）
     */
    private String dayRule;

    /**
     * 活动开始时间，格式 HH:mm 或 HH:mm:ss（可选）
     */
    private String startTime;

    /**
     * 持续时长（分钟），必须大于0（可选）
     */
    private Integer durationMin;

    /**
     * 活动地点（可选）
     */
    private String location;

    /**
     * 活动内容（可选）
     */
    private String action;

    /**
     * 参与者列表（JSON数组字符串，可选）
     */
    private String participants;

    /**
     * 扩展信息（JSON对象字符串，可选）
     */
    private String extra;
}
