package com.bqsummer.configuration;

import com.bqsummer.plugin.configplus.annotation.AppConfig;
import com.bqsummer.plugin.configplus.annotation.ConfigEle;
import lombok.Getter;

import java.util.Map;

@AppConfig(name = "common")
@Getter
public class CommonConfigs {

    @ConfigEle(name = "config_catalog")
    public Map<String, String> configCatalogs;
}
