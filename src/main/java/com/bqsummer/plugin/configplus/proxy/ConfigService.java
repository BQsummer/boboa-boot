package com.bqsummer.plugin.configplus.proxy;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.vo.Response;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.framework.ThreadLocalPool;
import com.bqsummer.plugin.configplus.annotation.AppConfig;
import com.bqsummer.plugin.configplus.annotation.ConfigEle;
import com.bqsummer.plugin.configplus.domain.*;
import com.bqsummer.plugin.configplus.mapper.ConfigHistoryMapper;
import com.bqsummer.plugin.configplus.mapper.ConfigMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ConfigService {

    public ConfigService(String appName, String env, ConfigMapper configMapper, ConfigHistoryMapper configHistoryMapper, ApplicationContext applicationContext) {
        this.appName = appName;
        this.env = env;
        this.configMapper = configMapper;
        this.configHistoryMapper = configHistoryMapper;
        this.applicationContext = applicationContext;
    }

    private final String appName;

    private final String env;

    private final ConfigMapper configMapper;

    private ConfigHistoryMapper configHistoryMapper;

    private final ApplicationContext applicationContext;

    public static Map<String, String> methodNameMapToConfig = new HashMap<>();

    public static Map<String, Class<?>> methodNameMapToReturnType = new HashMap<>();

    public static void initMethodMap(Field[] fields, BeanDefinitionHolder holder) throws ClassNotFoundException {
        if (fields.length > 0) {
            AppConfig appConfig = Class.forName(holder.getBeanDefinition().getBeanClassName()).getAnnotation(AppConfig.class);
            for (Field field : fields) {
                ConfigEle configEle = field.getAnnotation(ConfigEle.class);
                methodNameMapToConfig.put(buildMapKey(appConfig.name(), field.getName()), configEle.name());
                methodNameMapToReturnType.put(buildMapKey(appConfig.name(), field.getName()), field.getType());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfig(String configTypeName, String methodName) {
        Cache<String, Object> configCache = applicationContext.getBean(Cache.class);
        String fieldName = getFieldFromMethodName(methodName);
        String configName = methodNameMapToConfig.get(buildMapKey(configTypeName, fieldName));
        if (configName != null) {
            T value = (T) configCache.get(configName, e -> this.getConfigFromDB(configName, methodNameMapToReturnType.get(fieldName)));
            log.info("get config value of {} : {}", configName, value);
            return value;
        }
        return null;
    }

    public <T> Object getConfigFromDB(String configName, Class<T> returnType) {
        try {
            Config config = configMapper.getConfigFromDB(env, appName, configName);
            if (config != null) {
                switch (ConfigType.valueOf(config.getType())) {
                    case STRING:
                        return config.getValue();
                    case INTEGER:
                        return Integer.valueOf(config.getValue());
                    case DOUBLE:
                        return Double.valueOf(config.getValue());
                    case MAP:
                        return JSON.parseObject(config.getValue(), returnType);
                    default:
                        return null;
                }
            }
        } catch (Exception e) {
            log.error("get config from db failed. config = {}", configName, e);
        }
        return null;
    }

    public String getFieldFromMethodName(String methodName) {
        if (methodName.startsWith("get")) {
            char[] methodChars = methodName.toCharArray();
            return subPrefix(methodChars, 3);
        }
        if (methodName.startsWith("is")) {
            char[] methodChars = methodName.toCharArray();
            return subPrefix(methodChars, 2);
        }
        return null;
    }

    public String subPrefix(char[] methodChars, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < methodChars.length; i++) {
            if (i == startIndex) {
                sb.append(Character.toLowerCase(methodChars[i]));
            } else {
                sb.append(methodChars[i]);
            }
        }
        return sb.toString();
    }

    public static String buildMapKey(String configTypeName, String configName) {
        return configTypeName + "#" + configName;
    }

    public Page<Config> getAllConfigByPage(int pageNum, int pageSize, Config queryObject) {
        Page<Config> pagination = new Page<>(pageNum, pageSize);
        QueryWrapper<Config> queryWrapper = new QueryWrapper<Config>()
                .orderBy(true, false, "updated_at");

        if (StringUtils.isNotBlank(queryObject.getName())) {
            queryWrapper.eq("name", queryObject.getName());
        }
        if (StringUtils.isNotBlank(queryObject.getCatalog())) {
            queryWrapper.eq("catalog", queryObject.getCatalog());
        }
        return configMapper.selectPage(pagination, queryWrapper);
    }

    public void updateConfigById(Config config) {
        configMapper.updateById(config);
    }

    public void disableConfig(long id) {
        Config updateObj = Config.builder().id(id).status(ConfigStatus.INACTIVE).build();
        configMapper.updateById(updateObj);
    }

    public void enableConfig(long id) {
        Config updateObj = Config.builder().id(id).status(ConfigStatus.ACTIVE).build();
        configMapper.updateById(updateObj);
    }

    @Transactional(rollbackFor = Throwable.class)
    public Response updateValue(UpdateConfigReq req) {
        Config old = configMapper.selectById(req.getId());
        if (old == null) {
            throw new SnorlaxClientException("config not found");
        }

        Config config = new Config();
        BeanUtils.copyProperties(req, config);
        config.setUpdatedAt(LocalDateTime.now());
        config.setUpdatedBy(ThreadLocalPool.getUser());
        configMapper.updateById(config);

        ConfigHistory configHistory = new ConfigHistory();
        BeanUtils.copyProperties(old, configHistory);
        configHistoryMapper.insert(configHistory);
        return Response.success();
    }

    public Response addConfig(CreateConfigReq req) {
        Config config = new Config();
        BeanUtils.copyProperties(req, config);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        config.setCreatedBy(ThreadLocalPool.getUser());
        config.setUpdatedBy(ThreadLocalPool.getUser());
        configMapper.insert(config);
        return Response.success();
    }

    public Map<String, String> getAllConfigType() {
        return Arrays.stream(ConfigType.values()).collect(Collectors.toMap(Enum::name, ConfigType::getValue));
    }
}
