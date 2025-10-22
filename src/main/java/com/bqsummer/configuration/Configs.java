package com.bqsummer.configuration;

import com.bqsummer.service.configplus.annotation.AppConfig;
import com.bqsummer.service.configplus.annotation.ConfigEle;
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
    public Integer queueSize;
}
