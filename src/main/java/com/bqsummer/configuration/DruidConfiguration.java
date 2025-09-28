package com.bqsummer.configuration;

import com.alibaba.druid.support.jakarta.StatViewServlet;
import com.alibaba.druid.support.jakarta.WebStatFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DruidConfiguration {

    @Bean
    public ServletRegistrationBean druidStatViewServlet() {
        ServletRegistrationBean<StatViewServlet> servletRegistrationBean = new ServletRegistrationBean(
                new StatViewServlet(), "/druid/*");

        servletRegistrationBean.addInitParameter("loginUsername", "bqsummer");
        servletRegistrationBean.addInitParameter("loginPassword", "1qaz@WSX");
        servletRegistrationBean.addInitParameter("resetEnable", "false");
        return servletRegistrationBean;
    }

    @Bean
    public FilterRegistrationBean druidStatFilter() {

        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(
                new WebStatFilter());

        //  Add filtering rules.
        filterRegistrationBean.addUrlPatterns("/*");

        //  Adding a format information that does not need to be ignored.
        filterRegistrationBean.addInitParameter("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
        return filterRegistrationBean;
    }
}
