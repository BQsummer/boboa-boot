package com.bqsummer.plugin.configplus.proxy;

import com.bqsummer.plugin.configplus.annotation.AppConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class ConfigMethodInterceptor implements MethodInterceptor {

    @Autowired
    private ConfigService configService;

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        if (StringUtils.startsWith(method.getName(), "get") || StringUtils.startsWith(method.getName(), "is")) {
            AppConfig appConfig = obj.getClass().getSuperclass().getAnnotation(AppConfig.class);
            return configService.getConfig(appConfig.name(), method.getName());
        }
        return null;
    }


}
