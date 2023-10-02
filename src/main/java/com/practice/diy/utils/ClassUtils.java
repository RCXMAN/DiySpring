package com.practice.diy.utils;

import com.practice.diy.annotation.Bean;
import com.practice.diy.annotation.Component;
import com.practice.diy.exception.BeanDefinitionException;
import jakarta.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClassUtils {
    public static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annoClass) {
        A a = clazz.getAnnotation(annoClass);
        for (Annotation annotation : clazz.getAnnotations()) {
            Class<? extends Annotation> annoType = annotation.annotationType();
            if (!annoType.getPackageName().equals("java.lang.annotation")) {
                A found = findAnnotation(annoType, annoClass);
                if (found != null) {
                    if (a != null) {
                        throw new BeanDefinitionException(
                                "Duplicate @" + annoClass.getSimpleName() + " found on class " + clazz.getSimpleName());
                    }
                    a = found;
                }
            }
        }
        return a;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A getAnnotation(Annotation[] annotations, Class<A> annoClass) {
        for (Annotation anno : annotations) {
            if (annoClass.isInstance(anno)) {
                return (A) anno;
            }
        }
        return null;
    }

    public static String getBeanName(Class<?> clazz) {
        Component component = clazz.getAnnotation(Component.class);
        String name = "";
        if (component != null) {
            name = component.value();
        } else {
            for (Annotation anno : clazz.getAnnotations()) {
                if (findAnnotation(anno.annotationType(), Component.class) != null) {
                    try {
                        name = (String) anno.annotationType().getMethod("value").invoke(anno);
                    } catch (ReflectiveOperationException e) {
                        throw new BeanDefinitionException("Cannot get annotation value.", e);
                    }
                }
            }
        }
        if (name.isEmpty()) {
            name = clazz.getSimpleName();
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }
    public static String getBeanName(Method method) {
        Bean bean = method.getAnnotation(Bean.class);
        String name = bean.value();
        if (name.isEmpty()) {
            name = method.getName();
        }
        return name;
    }

    public static Method findAnnotationMethod(Class<?> clazz, Class<? extends Annotation> annoClass) {
        List<Method> methodList = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(annoClass))
                .map(m -> {
                    if (m.getParameters().length != 0) {
                        throw new BeanDefinitionException(
                                String.format("Method '%s' with @%s must have no argument: %s",
                                        m.getName(), annoClass.getSimpleName(), clazz.getName()));
                    }
                    return m;
                }).collect(Collectors.toList());
        if (methodList.size() > 1) {
            throw new BeanDefinitionException(String.format("Multiple methods with @%s found in class: %s", annoClass.getSimpleName(), clazz.getName()));
        }

        return methodList.isEmpty() ? null : methodList.get(0);
    }

    public static Method getMethodByName(Class<?> clazz, String methodName) {
        try {
            return clazz.getDeclaredMethod(methodName);
        } catch (ReflectiveOperationException e) {
            throw new BeanDefinitionException(String.format("Method '%s' not found in class: %s", methodName, clazz.getName()));
        }
    }
}
