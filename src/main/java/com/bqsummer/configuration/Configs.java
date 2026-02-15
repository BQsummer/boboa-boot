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

}
