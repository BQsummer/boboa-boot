package com.bqsummer.framework.job;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

public class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory implements ApplicationContextAware {

    private transient AutowireCapableBeanFactory beanFactory;
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(final ApplicationContext context) {
        this.applicationContext = context;
        beanFactory = context.getAutowireCapableBeanFactory();
    }

    @Override
    protected Object createJobInstance(final TriggerFiredBundle bundle) throws Exception {
        final Class<?> jobClass = bundle.getJobDetail().getJobClass();

        // Try to get the bean from Spring context first (for @Component jobs)
        try {
            return applicationContext.getBean(jobClass);
        } catch (Exception e) {
            // If not found in Spring context, fall back to creating new instance and autowiring
            final Object job = super.createJobInstance(bundle);
            beanFactory.autowireBean(job);
            return job;
        }
    }
}
