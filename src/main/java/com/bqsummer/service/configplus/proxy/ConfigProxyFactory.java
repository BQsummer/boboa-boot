package com.bqsummer.service.configplus.proxy;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ConfigProxyFactory<T> implements FactoryBean<T>, ApplicationContextAware {

    private final Class<T> clazz;

    private static ApplicationContext applicationContext;

    public ConfigProxyFactory(Class<T> clazz) {
        this.clazz = clazz;
    }

    public Object getProxyInstance() {
        Enhancer en = new Enhancer();
        en.setSuperclass(clazz);
        en.setCallback(applicationContext.getBean(ConfigMethodInterceptor.class));
        return en.create();
    }


    @SuppressWarnings("unchecked")
    @Override
    public T getObject() throws Exception {
        T instance = (T) new ConfigProxyFactory(clazz).getProxyInstance();
        applicationContext
                .getAutowireCapableBeanFactory()
                .autowireBean(instance);
        return instance;
    }

    @Override
    public Class<T> getObjectType() {
        return clazz;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ConfigProxyFactory.applicationContext = applicationContext;
    }
}