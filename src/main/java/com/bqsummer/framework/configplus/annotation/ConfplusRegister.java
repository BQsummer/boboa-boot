package com.bqsummer.framework.configplus.annotation;

import com.bqsummer.framework.configplus.proxy.ConfigProxyFactory;
import com.bqsummer.framework.configplus.proxy.ConfigService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Slf4j
public class ConfplusRegister implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, BeanClassLoaderAware, EnvironmentAware {

    private ResourceLoader resourceLoader;

    private ClassLoader classLoader;

    private Environment environment;

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @SneakyThrows
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        ConfigplusClassPathBeanDefinitionScanner scanner = new ConfigplusClassPathBeanDefinitionScanner(registry, false);
        Set<String> packagesToScan = this.getPackagesToScan(importingClassMetadata);
        Set<BeanDefinitionHolder> beans = scanner.doScan(packagesToScan.toArray(new String[]{}));
        if (!CollectionUtils.isEmpty(beans)) {
            for (BeanDefinitionHolder holder : beans) {
                // 初始化字段信息
                ConfigService.initMethodMap(Class.forName(holder.getBeanDefinition().getBeanClassName()).getDeclaredFields(), holder);

                GenericBeanDefinition beanDefinition = ((GenericBeanDefinition) holder.getBeanDefinition());
                //将bean的真实类型改变为FactoryBean
                beanDefinition.getConstructorArgumentValues().
                        addGenericArgumentValue(Objects.requireNonNull(beanDefinition.getBeanClassName()));
                beanDefinition.setBeanClass(ConfigProxyFactory.class);
                beanDefinition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
            }
        }
    }

    private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
        AnnotationAttributes attributes =
                AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(ConfigplusScan.class.getName()));
        assert attributes != null;
        String[] basePackages = attributes.getStringArray("basePackages");

        Set<String> packagesToScan = new LinkedHashSet<>(Arrays.asList(basePackages));

        if (packagesToScan.isEmpty()) {
            packagesToScan.add(ClassUtils.getPackageName(metadata.getClassName()));
        }

        return packagesToScan;
    }

}
