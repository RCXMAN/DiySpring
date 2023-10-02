package com.practice.diy.context;


import com.practice.diy.annotation.*;
import com.practice.diy.exception.*;
import com.practice.diy.io.PropertyResolver;
import com.practice.diy.io.ResourceResolver;
import com.practice.diy.utils.ClassUtils;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class AnnotationConfigApplicationContext implements ConfigurableApplicationContext {
    private final PropertyResolver propertyResolver;
    private final Map<String, BeanDefinition> beans;

    private Set<String> createdBeanNames;

    public AnnotationConfigApplicationContext(PropertyResolver propertyResolver, Class<?> configClass) throws IOException {
        ApplicationContextUtils.setApplicationContext(this);
        this.propertyResolver = propertyResolver;
        final Set<String> beanClassNames = scanForClassNames(configClass);

        this.beans = createBeanDefinitions(beanClassNames);

        this.createdBeanNames = new HashSet<>();

        this.beans.values()
                .stream()
                .filter(this::isConfigurationDefinition)
                .sorted()
                .forEach(beanDefinition -> createBeanAsEarlySingleton(beanDefinition));

        createNormalBeans();

        this.beans.values().forEach(def -> injectBean(def));
        this.beans.values().forEach(def -> initBean(def));
    }


    @Override
    public boolean containsBean(String beanName) {
        return this.beans.containsKey(beanName);
    }

    public <T> T getBean(String name) {
        BeanDefinition definition = this.beans.get(name);
        if (definition == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s'.", name));
        }
        return (T) definition.getRequiredInstance();
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        BeanDefinition definition = findBeanDefinition(requiredType);
        if (definition == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with type '%s'.", requiredType));
        }
        return (T) definition.getRequiredInstance();
    }

    @Override
    public <T> T getBean(String beanName, Class<T> requiredType) {
        BeanDefinition beanDefinition = findBeanDefinition(requiredType);
        if (beanDefinition == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with type '%s'.", requiredType));
        }
        return (T) beanDefinition.getRequiredInstance();
    }

    @Override
    public <T> List<T> getBeans(Class<T> requiredType) {
        List<BeanDefinition> beanDefinitions = findBeanDefinitions(requiredType);
        if (beanDefinitions.isEmpty()) {
            return List.of();
        }

        List<T> list = new ArrayList<>(beanDefinitions.size());
        for (var def : beanDefinitions) {
            list.add((T) def.getRequiredInstance());
        }

        return list;
    }

    @Nullable
    public BeanDefinition findBeanDefinition(String beanName) {
        return beans.get(beanName);
    }

    @Nullable
    public BeanDefinition findBeanDefinition(Class<?> requiredType) {
        List<BeanDefinition> beanDefinitionList = findBeanDefinitions(requiredType);
        if (beanDefinitionList.isEmpty()) {
            return null;
        }

        if (beanDefinitionList.size() > 1) {
            beanDefinitionList = beanDefinitionList
                    .stream()
                    .filter(def -> def.isPrimary())
                    .collect(Collectors.toList());
        }

        if (beanDefinitionList.isEmpty()) {
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found", requiredType.getName()));
        } else if (beanDefinitionList.size() > 1) {
            throw new NoUniqueBeanDefinitionException(
                    String.format("Multiple bean with type '%s' found, and multiple @Primary specified.", requiredType.getName()));
        }

        return beanDefinitionList.get(0);
    }

    @Nullable
    public BeanDefinition findBeanDefinition(String beanName, Class<?> requiredType) {
        BeanDefinition beanDefinition = findBeanDefinition(beanName);
        if (requiredType == null) {
            return null;
        }
        if (!requiredType.isAssignableFrom(beanDefinition.getBeanClass())) {
            throw new BeanNotOfRequiredTypeException(
                    String.format("Autowire required type '%s' but bean '%s' has actual type '%s'.",
                            requiredType.getName(),
                            beanName, beanDefinition.getBeanClass().getName()));
        }
        return beanDefinition;
    }

    public List<BeanDefinition> findBeanDefinitions(Class<?> requiredType) {
        return this.beans.values().stream()
                .filter(def -> requiredType.isAssignableFrom(def.getBeanClass()))
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
        this.beans.values().forEach(beanDefinition -> {
            callMethod(beanDefinition.getInstance(), beanDefinition.getDestroyMethod(), beanDefinition.getDestroyMethodName());
        });
        this.beans.clear();
        ApplicationContextUtils.setApplicationContext(null);
    }

    public Object createBeanAsEarlySingleton(BeanDefinition beanDefinition) {
        if (!this.createdBeanNames.add(beanDefinition.getName())) {
            throw new UnsatisfiedDependencyException(
                    String.format("Circular dependency detected when create bean '%s'", beanDefinition.getName()));
        }

        Executable createFunction = null;
        if (beanDefinition.getFactoryName() == null) {
            createFunction = beanDefinition.getConstructor();
        } else {
            createFunction = beanDefinition.getFactoryMethod();
        }

        final Parameter[] parameters = createFunction.getParameters();
        final Annotation[][] parameterAnnotations = createFunction.getParameterAnnotations();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            final Parameter param = parameters[i];
            final Annotation[] annos = parameterAnnotations[i];
            final Value value = ClassUtils.getAnnotation(annos, Value.class);
            final Autowired autowired = ClassUtils.getAnnotation(annos, Autowired.class);

            final boolean isConfiguration = isConfigurationDefinition(beanDefinition);

            if (isConfiguration && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify @Autowired when create @Configuration bean '%s': %s.",
                                beanDefinition.getName(), beanDefinition.getBeanClass().getName()));
            }

            if (value != null && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify both @Autowired and @Value when create bean '%s': %s.",
                                beanDefinition.getName(), beanDefinition.getBeanClass().getName()));
            }

            if (value == null && autowired == null) {
                throw new BeanCreationException(
                        String.format("Must specify @Autowired or @Value when create bean '%s': %s.",
                                beanDefinition.getName(), beanDefinition.getBeanClass().getName()));
            }

            final Class<?> type = param.getType();
            if (value != null) {
                args[i] = this.propertyResolver.getRequiredProperty(value.value(), type);
            } else {
                boolean required = autowired.required();
                String qualifier = autowired.qualifier();
                BeanDefinition dependDef = qualifier.isEmpty() ? findBeanDefinition(type) : findBeanDefinition(qualifier, type);

                if (required && dependDef == null) {
                    throw new BeanCreationException(
                            String.format("Missing autowired bean with type '%s' when create bean '%s': %s.", type.getName(),
                                    beanDefinition.getName(), beanDefinition.getBeanClass().getName()));
                }

                if (dependDef != null && !isConfiguration) {
                    Object autowiredBeanInstance = dependDef.getInstance();
                    if (autowiredBeanInstance == null) {
                        autowiredBeanInstance = createBeanAsEarlySingleton(dependDef);
                    }
                    args[i] = autowiredBeanInstance;
                } else {
                    args[i] = null;
                }
            }
        }

        Object instance = null;
        if (beanDefinition.getFactoryName() == null) {
            try {
                instance = beanDefinition.getConstructor().newInstance(args);
            } catch (Exception e) {
                throw new BeanCreationException(
                        String.format("Exception when create bean '%s': %s",
                                beanDefinition.getName(), beanDefinition.getBeanClass().getName()), e);
            }
        } else {
            Object configInstance = getBean(beanDefinition.getFactoryName());
            try {
                instance = beanDefinition.getFactoryMethod().invoke(configInstance, args);
            } catch (Exception e) {
                throw new BeanCreationException(
                        String.format("Exception when create bean '%s': %s",
                                beanDefinition.getName(), beanDefinition.getBeanClass().getName()), e);
            }
        }
        beanDefinition.setInstance(instance);
        return beanDefinition.getInstance();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(String name, Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(name, requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> List<T> findBeans(Class<T> requiredType) {
        return findBeanDefinitions(requiredType).stream().map(def -> (T) def.getRequiredInstance()).collect(Collectors.toList());
    }

    private boolean isConfigurationDefinition(BeanDefinition def) {
        return ClassUtils.findAnnotation(def.getBeanClass(), Configuration.class) != null;
    }

    private Set<String> scanForClassNames(Class<?> configClass) throws IOException {
        ComponentScan componentScan = configClass.getAnnotation(ComponentScan.class);
        final String[] scanPackages = componentScan == null || componentScan.value().length == 0
                ? new String[] {configClass.getPackage().getName()} : componentScan.value();

        Set<String> classNameSet = new HashSet<>();

        for (String pkg : scanPackages) {
            ResourceResolver resourceResolver = new ResourceResolver(pkg);
            List<String> classList = resourceResolver.scan(resource -> {
                String name = resource.name();
                if (name.endsWith(".class")) {
                    return name.substring(0, name.length() - 6)
                            .replace("/", ".")
                            .replace("\\", ".");
                }
                return null;
            });
            classNameSet.addAll(classList);
        }

        Import importAnno = configClass.getAnnotation(Import.class);
        if (importAnno != null) {
            for (Class<?> importClass : importAnno.value()) {
                classNameSet.add(importClass.getName());
            }
        }

        return classNameSet;
    }

    private Map<String, BeanDefinition> createBeanDefinitions(Set<String> beanClassNames) {
        Map<String, BeanDefinition> definitionMap = new HashMap<>();
        for (String className : beanClassNames) {
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new BeanCreationException(e);
            }

            if (clazz.isAnnotation() || clazz.isEnum() || clazz.isInterface() || clazz.isRecord()) {
                continue;
            }

            Component component = ClassUtils.findAnnotation(clazz, Component.class);
            if (component != null) {
                int mod = clazz.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be abstract.");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be private.");
                }

                String beanName = ClassUtils.getBeanName(clazz);

                BeanDefinition beanDefinition = new BeanDefinition(
                        beanName, clazz, getSuitableConstructor(clazz),
                        getOrder(clazz), clazz.isAnnotationPresent(Primary.class),
                        null, null,
                        ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                        ClassUtils.findAnnotationMethod(clazz, PreDestroy.class)
                );

                addBeanDefinitions(definitionMap, beanDefinition);

                Configuration configuration = ClassUtils.findAnnotation(clazz, Configuration.class);
                if (configuration != null) {
                    scanFactoryMethods(beanName, clazz, definitionMap);
                }
            }
        }
        return definitionMap;
    }

    private void scanFactoryMethods(String factoryBeanName, Class<?> clazz, Map<String, BeanDefinition> definitionMap) {
        for (Method method : clazz.getDeclaredMethods()) {
            Bean bean = method.getAnnotation(Bean.class);

            if (bean != null) {
                int mod = method.getModifiers();
                if (Modifier.isAbstract(mod) || Modifier.isFinal(mod) || Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName()
                            + " modifier is not valid.");
                }

                Class<?> beanClass = method.getReturnType();
                if (beanClass.isPrimitive() || beanClass == void.class || beanClass == Void.class) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName()
                            + " return type is not valid.");
                }

                BeanDefinition beanDefinition = new BeanDefinition(
                        ClassUtils.getBeanName(method), beanClass, factoryBeanName, method,
                        getOrder(method), method.isAnnotationPresent(Primary.class),
                        bean.initMethod().isEmpty() ? null : bean.initMethod(),
                        bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),
                        null, null
                );

                addBeanDefinitions(definitionMap, beanDefinition);
            }
        }
    }

    private void addBeanDefinitions(Map<String, BeanDefinition> definitionMap, BeanDefinition beanDefinition) {
        if (definitionMap.put(beanDefinition.getName(), beanDefinition) != null) {
            throw new BeanDefinitionException("Duplicate bean name: " + beanDefinition.getName());
        }
    }

    private Constructor<?> getSuitableConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length == 0) {
            constructors = clazz.getDeclaredConstructors();
        }
        if (constructors.length != 1) {
            throw new BeanDefinitionException("More than one constructor found in class " + clazz.getName() + ".");
        }

        return constructors[0];
    }

    private int getOrder(Class<?> clazz) {
        Order order = clazz.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    private int getOrder(Method method) {
        Order order = method.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    private void createNormalBeans() {
        List<BeanDefinition> beanDefinitions = this.beans.values()
                .stream()
                .filter(def -> def.getInstance() == null)
                .sorted()
                .collect(Collectors.toList());
        beanDefinitions.forEach(def -> {
            if (def.getInstance() == null) {
                createBeanAsEarlySingleton(def);
            }
        });
    }

    void injectBean(BeanDefinition def) {
        try {
            injectProperties(def.getBeanClass(), def.getInstance());
        } catch (ReflectiveOperationException e) {
            throw new BeanCreationException(e);
        }
    }

    private void injectProperties (Class<?> beanClass, Object instance) throws InvocationTargetException, IllegalAccessException {
        for (Field field : beanClass.getDeclaredFields()) {
            tryInjectProperties(beanClass, instance, field);
        }

        for (Method method : beanClass.getDeclaredMethods()) {
            tryInjectProperties(beanClass, instance, method);
        }

        Class<?> superBeanClass = beanClass.getSuperclass();

        if (superBeanClass != null) {
            injectProperties(superBeanClass, instance);
        }
    }

    private void tryInjectProperties(
            Class<?> beanClass, Object instance, AccessibleObject accessibleObject) throws IllegalAccessException, InvocationTargetException {
        Value value = accessibleObject.getAnnotation(Value.class);
        Autowired autowired = accessibleObject.getAnnotation(Autowired.class);
        if (value == null && autowired == null) {
            return;
        }

        if (value != null && autowired != null) {
            throw new BeanCreationException(String.format("Cannot specify both @Autowired and @Value when inject %s for bean '%s'",
                    beanClass.getSimpleName(), beanClass.getName()));
        }

        Field field = null;
        Method method = null;

        if (accessibleObject instanceof Field f) {
            checkModifier(f);
            f.setAccessible(true);
            field = f;
        }

        if (accessibleObject instanceof Method m) {
            checkModifier(m);
            if (m.getParameters().length != 1) {
                throw new BeanDefinitionException(
                        String.format("Cannot inject a non-setter method %s for bean '%s'",
                                m.getName(), beanClass.getName()));
            }

            m.setAccessible(true);
            method = m;
        }

        String accessibleName = field != null ? field.getName() : method.getName();
        Class<?> accessibleType = field != null ? field.getType() : method.getParameterTypes()[0];

        if (value != null) {
            Object propValue = this.propertyResolver.getProperty(value.value(), accessibleType);
            if (field != null) {
                field.set(instance, propValue);
            }
            if (method != null) {
                method.invoke(instance, propValue);
            }
        }

        if (autowired != null) {
            boolean required = autowired.required();
            String qualifier = autowired.qualifier();
            Object dependBean = qualifier.isEmpty() ? findBean(accessibleType) : findBean(qualifier, accessibleType);

            if (required && dependBean == null) {
                throw new UnsatisfiedDependencyException(
                        String.format("Dependency bean not found when inject %s.%s for bean '%s'",
                                beanClass.getSimpleName(), accessibleName,
                                beanClass.getName()));
            }

            if (dependBean != null) {
                if (field != null) {
                    field.set(instance, dependBean);
                }
                if (method != null) {
                    method.invoke(instance, dependBean);
                }
            }
        }
    }
    private void checkModifier(Member m) {
        int mod = m.getModifiers();
        if (Modifier.isStatic(mod)) {
            throw new BeanDefinitionException("Cannot inject static field: " + m);
        }
        if (Modifier.isFinal(mod)) {
            if (m instanceof Field field) {
                throw new BeanDefinitionException("Cannot inject final field: " + field);
            }
        }
    }
    private void initBean(BeanDefinition beanDefinition) {
        callMethod(beanDefinition.getInstance(), beanDefinition.getInitMethod(), beanDefinition.getInitMethodName());
    }
    private void callMethod(Object beanInstance, Method method, String methodName) {
        if (method != null) {
            try {
                method.invoke(beanInstance);
            } catch (ReflectiveOperationException e) {
                throw new BeanCreationException(e);
            }
        } else if (methodName != null) {
            Method initMethod = ClassUtils.getMethodByName(beanInstance.getClass(), methodName);
            initMethod.setAccessible(true);
            try {
                initMethod.invoke(beanInstance);
            } catch (ReflectiveOperationException e) {
                throw new BeanCreationException(e);
            }
        }
    }
}
