package com.bqsummer.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "message.center")
@Getter
@Setter
public class MessageCenterProperties {

    private String smtpHost;

    private Integer smtpPort;

    private String account;

    private String password;

    private String smtpTimeout = "60000";

    private String smtpConnectTimeout = "60000";
}
