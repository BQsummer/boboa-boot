package com.bqsummer.configuration;

import com.bqsummer.plugin.configplus.annotation.AppConfig;
import com.bqsummer.plugin.configplus.annotation.ConfigEle;
import lombok.Getter;

@AppConfig(name = "con")
@Getter
public class Configs {

    @ConfigEle(name = "test")
    public String testValue;
}
