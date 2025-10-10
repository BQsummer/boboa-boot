package com.bqsummer.configuration;

import com.bqsummer.service.configplus.annotation.AppConfig;
import com.bqsummer.service.configplus.annotation.ConfigEle;
import lombok.Getter;

@AppConfig(name = "con")
@Getter
public class Configs {

    @ConfigEle(name = "test")
    public String testValue;
}
