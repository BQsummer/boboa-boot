package com.bqsummer.configuration;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Primary
@Configuration
public class DalConfiguration {

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        return DruidDataSourceBuilder.create().build();
    }
}
