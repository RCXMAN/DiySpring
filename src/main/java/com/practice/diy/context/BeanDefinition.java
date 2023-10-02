package com.practice.diy.context;

import com.practice.diy.exception.BeanCreationException;
import jakarta.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

public class BeanDefinition implements Comparable<BeanDefinition>{
    private final String name;
    private final Class<?> beanClass;
    private Object instance = null;
    private final Constructor<?> constructor;
    private final String factoryName;
    private final Method factoryMethod;
    private final int order;
    private final boolean primary;

    private String initMethodName;
    private String destroyMethodName;

    private Method initMethod;
    private Method destroyMethod;

    public BeanDefinition(String name, Class<?> beanClass, Constructor<?> constructor, int order, boolean primary,
                          String initMethodName, String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.name = name;
        this.beanClass = beanClass;
        this.constructor = constructor;
        this.factoryName = null;
        this.factoryMethod = null;
        this.order = order;
        this.primary = primary;
        constructor.setAccessible(true);
        setInitAndDestroyMethod(initMethodName, destroyMethodName, initMethod, destroyMethod);
    }

    public BeanDefinition(String name, Class<?> beanClass, String factoryName, Method factoryMethod,
                          int order, boolean primary,
                          String initMethodName, String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.name = name;
        this.beanClass = beanClass;
        this.constructor = null;
        this.factoryName = factoryName;
        this.factoryMethod = factoryMethod;
        this.order = order;
        this.primary = primary;
        factoryMethod.setAccessible(true);
        setInitAndDestroyMethod(initMethodName, destroyMethodName, initMethod, destroyMethod);
    }

    private void setInitAndDestroyMethod(String initMethodName, String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.initMethodName = initMethodName;
        this.destroyMethodName = destroyMethodName;
        if (initMethod != null) {
            initMethod.setAccessible(true);
        }
        if (destroyMethod != null) {
            destroyMethod.setAccessible(true);
        }
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }

    public String getName() {
        return this.name;
    }

    public Class<?> getBeanClass() {
        return this.beanClass;
    }

    @Nullable
    public Object getInstance() {
        return this.instance;
    }

    @Nullable
    public Constructor<?> getConstructor() {
        return this.constructor;
    }

    @Nullable
    public String getFactoryName() {
        return this.factoryName;
    }

    @Nullable
    public Method getFactoryMethod() {
        return this.factoryMethod;
    }

    @Nullable
    public Method getInitMethod() {
        return this.initMethod;
    }

    @Nullable
    public Method getDestroyMethod() {
        return this.destroyMethod;
    }

    @Nullable
    public String getInitMethodName() {
        return this.initMethodName;
    }

    @Nullable
    public String getDestroyMethodName() {
        return this.destroyMethodName;
    }

    public boolean isPrimary() {
        return this.primary;
    }

    public Object getRequiredInstance() {
        if (this.instance == null) {
            throw new BeanCreationException(String.format("Instance of bean with name '%s' and type '%s' is not instantiated during current stage.",
                    this.getName(), this.getBeanClass().getName()));
        }
        return this.instance;
    }

    public void setInstance(Object instance) {
        Objects.requireNonNull(instance, "Bean instance is null");
        if (!this.beanClass.isAssignableFrom(instance.getClass())) {
            throw new BeanCreationException(
                    String.format("Instance '%s' of Bean '%s' is not the expected type: '%s'",
                            instance, instance.getClass().getName(), this.beanClass.getName())
            );
        }
        this.instance = instance;
    }

    @Override
    public int compareTo(BeanDefinition beanDefinition) {
        int c = Integer.compare(this.order, beanDefinition.order);
        if (c != 0) {
            return c;
        }
        return this.name.compareTo(beanDefinition.name);
    }
}
