package com.bqsummer.configuration;

import com.bqsummer.framework.configplus.annotation.AppConfig;
import com.bqsummer.framework.configplus.annotation.ConfigEle;
import lombok.Getter;

@AppConfig(name = "con")
@Getter
public class Configs {

    /**
     * 不限流的ip白名单，多个ip用逗号分隔
     */
    @ConfigEle(name = "ipWhiteList")
    public String ipWhiteList;

    /**
     * 限流黑名单，多个ip用逗号分隔
     */
    @ConfigEle(name = "ipBlackList")
    public String ipBlackList;

    /**
     * 任务队列大小
     */
    @ConfigEle(name = "queueSize")
    public Integer queueSize = 1000;

    /**
     * 并发延迟时间，单位秒
     */
    @ConfigEle(name = "concurrentDelay")
    public Integer concurrentDelay = 1;

    /**
     * 重试间隔时间，单位秒，逗号分隔多个重试间隔
     */
    @ConfigEle(name = "retryDelay")
    public String retryDelay = "60,60,60,120";

    /**
     * 任务超时时间，单位秒
     */
    @ConfigEle(name = "timeoutTask")
    public Integer timeoutTask = 180;

    /**
     * 最大连续失败次数
     */
    @ConfigEle(name = "maxConsecutiveFailures")
    public Integer maxConsecutiveFailures = 3;

    /**
     * 邀请码长度
     */
    @ConfigEle(name = "inviteCodeLength")
    public Integer inviteCodeLength = 10;

    /**
     * 自动创建邀请码时的默认可用次数
     */
    @ConfigEle(name = "inviteDefaultMaxUses")
    public Integer inviteDefaultMaxUses = 1;

    /**
     * 自动创建邀请码时的默认有效天数
     */
    @ConfigEle(name = "inviteDefaultExpireDays")
    public Integer inviteDefaultExpireDays = 30;

    /**
     * 关系分每日入账上限
     */
    @ConfigEle(name = "daily_points_cap")
    public Integer dailyPointsCap = 30;

    /**
     * 关系分每日重置时区
     */
    @ConfigEle(name = "daily_reset_timezone")
    public String dailyResetTimezone = "Asia/Shanghai";

    /**
     * 升级冷却时长(小时)
     */
    @ConfigEle(name = "upgrade_cooldown_hours")
    public Integer upgradeCooldownHours = 24;

    /**
     * 超过该天数未互动开始衰减
     */
    @ConfigEle(name = "decay_after_inactive_days")
    public Integer decayAfterInactiveDays = 7;

    /**
     * 衰减速率(每天衰减分)
     */
    @ConfigEle(name = "decay_points_per_day")
    public Integer decayPointsPerDay = 3;

    /**
     * 特殊情绪一次性奖励最大次数
     */
    @ConfigEle(name = "special_emotion_max_rewards")
    public Integer specialEmotionMaxRewards = 3;

    /**
     * 连续对话轮数阈值
     */
    @ConfigEle(name = "long_turns_threshold")
    public Integer longTurnsThreshold = 12;

    /**
     * 构建提示词时加载的历史消息条数
     */
    @ConfigEle(name = "prompt_history_message_limit")
    public Integer promptHistoryMessageLimit = 20;

}
