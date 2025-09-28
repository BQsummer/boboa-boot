package com.bqsummer.listener;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.stereotype.Component;

import java.util.Properties;


public class CustomerApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    public CustomerApplicationListener() {
    }

    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        String appName = environment.getProperty("spring.application.name");
        Properties fristProps = new Properties();
        fristProps.put("server.servlet.context-path", "/" + appName);
        environment.getPropertySources().addFirst(new PropertiesPropertySource("first-mvc-starter", fristProps));
        Properties lastProps = new Properties();
        lastProps.put("server.compression.enabled", "true");
        lastProps.put("server.compression.min-response-size", "2048");
        lastProps.put("server.compression.mime-types", "text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json,application/xml");
        lastProps.put("server.connection-timeout", 35000);
        lastProps.put("server.tomcat.max-threads", 500);
        lastProps.put("server.tomcat.min-spare-threads", 25);
        environment.getPropertySources().addLast(new PropertiesPropertySource("last-mvc-starter", lastProps));
    }
}