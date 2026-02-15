package com.bqsummer.service;

import com.bqsummer.common.dto.config.Config;
import com.bqsummer.common.vo.req.config.SaveIpManageReq;
import com.bqsummer.common.vo.resp.config.IpManageConfigResp;
import com.bqsummer.framework.configplus.proxy.ConfigService;
import com.bqsummer.mapper.ConfigMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IpBlacklistConfigService {

    private static final String CONFIG_TYPE_NAME = "con";
    private static final String IP_WHITE_LIST_NAME = "ipWhiteList";
    private static final String IP_BLACK_LIST_NAME = "ipBlackList";

    @Value("${spring.profiles.active}")
    private String env;

    @Value("${spring.application.name}")
    private String appName;

    private final ConfigMapper configMapper;

    private final Cache<String, Object> configCache;

    public IpManageConfigResp getIpManageConfig() {
        return new IpManageConfigResp(readConfigValue(IP_WHITE_LIST_NAME), readConfigValue(IP_BLACK_LIST_NAME));
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveIpManageConfig(SaveIpManageReq req) {
        if (req == null) {
            saveConfigValue(IP_WHITE_LIST_NAME, "");
            saveConfigValue(IP_BLACK_LIST_NAME, "");
            return;
        }

        saveConfigValue(IP_WHITE_LIST_NAME, req.getIpWhiteList());
        saveConfigValue(IP_BLACK_LIST_NAME, req.getIpBlackList());
    }

    private void saveConfigValue(String configName, String rawValue) {
        String normalizedValue = normalize(rawValue);
        Config existing = findCurrentConfig(configName);
        LocalDateTime now = LocalDateTime.now();

        if (existing == null) {
            configMapper.insertConfigForPostgres(
                    env,
                    appName,
                    configName,
                    normalizedValue,
                    "STRING",
                    "false",
                    "ACTIVE",
                    "system",
                    now,
                    now,
                    "",
                    ""
            );
        } else {
            configMapper.updateConfigValueById(existing.getId(), normalizedValue, now, "");
        }

        String cacheKey = ConfigService.buildMapKey(CONFIG_TYPE_NAME, configName);
        configCache.invalidate(cacheKey);
    }

    private String readConfigValue(String configName) {
        Config config = findCurrentConfig(configName);
        return config == null || config.getValue() == null ? "" : config.getValue();
    }

    private Config findCurrentConfig(String configName) {
        return configMapper.findConfigIdAndValue(env, appName, configName);
    }

    private String normalize(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }

        Set<String> ipSet = Arrays.stream(rawValue.split("[,\\r\\n]+"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return String.join(",", ipSet);
    }
}
