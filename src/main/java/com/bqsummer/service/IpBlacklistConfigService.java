package com.bqsummer.service;

import com.bqsummer.common.dto.config.Config;
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
    private static final String CONFIG_NAME = "ipWhiteList";

    @Value("${spring.profiles.active}")
    private String env;

    @Value("${spring.application.name}")
    private String appName;

    private final ConfigMapper configMapper;

    private final Cache<String, Object> configCache;

    public String getIpBlacklistValue() {
        Config config = findCurrentConfig();
        return config == null || config.getValue() == null ? "" : config.getValue();
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveIpBlacklistValue(String rawValue) {
        String normalizedValue = normalize(rawValue);
        Config existing = findCurrentConfig();
        LocalDateTime now = LocalDateTime.now();

        if (existing == null) {
            configMapper.insertConfigForPostgres(
                    env,
                    appName,
                    CONFIG_NAME,
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

        String cacheKey = ConfigService.buildMapKey(CONFIG_TYPE_NAME, CONFIG_NAME);
        configCache.invalidate(cacheKey);
    }

    private Config findCurrentConfig() {
        return configMapper.findConfigIdAndValue(env, appName, CONFIG_NAME);
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
