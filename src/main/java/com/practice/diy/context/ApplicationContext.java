package com.practice.diy.context;

import java.util.List;

public interface ApplicationContext extends AutoCloseable{
    boolean containsBean(String beanName);
    <T> T getBean(String beanName);
    <T> T getBean(String beanName, Class<T> requiredType);
    <T> T getBean(Class<T> requiredType);
    <T> List<T> getBeans(Class<T> requiredType);
    void close();
}
