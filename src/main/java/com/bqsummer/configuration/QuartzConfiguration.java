package com.bqsummer.configuration;

import com.bqsummer.framework.job.AutowiringSpringBeanJobFactory;
import com.bqsummer.framework.job.JobExecutorRegister;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

@Configuration
public class QuartzConfiguration {

    @Bean
    public JobFactory jobFactory(ApplicationContext applicationContext) {
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(JobFactory jobFactory, Properties quartzProperties, DataSource dataSource) throws Exception {
        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        schedulerFactory.setQuartzProperties(quartzProperties);
        schedulerFactory.setWaitForJobsToCompleteOnShutdown(true);
        schedulerFactory.setAutoStartup(true);
        schedulerFactory.setJobFactory(jobFactory);
        schedulerFactory.setDataSource(dataSource);
        return schedulerFactory;
    }

    @Bean
    public Properties quartzProperties() throws IOException {
        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        propertiesFactoryBean.setLocation(new ClassPathResource("/quartz.properties"));
        propertiesFactoryBean.afterPropertiesSet();
        return propertiesFactoryBean.getObject();
    }


    //    @Bean
    public CronTriggerFactoryBean cronTriggerFactoryBean(JobExecutorRegister register, SchedulerFactoryBean factory) throws SchedulerException {
        CronTriggerFactoryBean factoryBean = new CronTriggerFactoryBean();
        register.registerAllJobs(factory);
        return factoryBean;
    }
}
