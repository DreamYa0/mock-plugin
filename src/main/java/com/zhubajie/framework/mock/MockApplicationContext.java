package com.zhubajie.framework.mock;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author dreamyao
 * @version 1.0.0
 */
public class MockApplicationContext implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 根据Class对象获取Bean
     * @param clazz Class对象
     * @param <T>   类型参数
     * @return Bean
     */
    public <T> T getBean(Class<T> clazz) {
        if (applicationContext == null) {
            throw new GetBeanException("获取ApplicationContext失败！");
        }
        return applicationContext.getBean(clazz);
    }
}
