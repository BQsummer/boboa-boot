package com.bqsummer.service.configplus.annotation;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import java.util.Set;

public class ConfigplusClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

    public ConfigplusClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters) {
        super(registry, useDefaultFilters);
        registerFilters();
    }

    protected void registerFilters() {
        addIncludeFilter(new AnnotationTypeFilter(AppConfig.class));
    }

    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        return super.doScan(basePackages);
    }
}