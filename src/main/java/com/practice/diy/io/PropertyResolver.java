package com.practice.diy.io;

import jakarta.annotation.Nullable;

import java.time.*;
import java.util.*;
import java.util.function.Function;

public class PropertyResolver {
    Map<String, String> properties = new HashMap<>();
    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();

    public PropertyResolver(Properties props) {
        this.properties.putAll(System.getenv());
        Set<String> names = props.stringPropertyNames();

        for (String name : names) {
            this.properties.put(name, props.getProperty(name));
        }

        converters.put(String.class, s -> s);
        converters.put(boolean.class, s -> Boolean.parseBoolean(s));
        converters.put(Boolean.class, s -> Boolean.valueOf(s));

        converters.put(byte.class, s -> Byte.parseByte(s));
        converters.put(Byte.class, s -> Byte.valueOf(s));

        converters.put(short.class, s -> Short.parseShort(s));
        converters.put(Short.class, s -> Short.valueOf(s));

        converters.put(int.class, s -> Integer.parseInt(s));
        converters.put(Integer.class, s -> Integer.valueOf(s));

        converters.put(long.class, s -> Long.parseLong(s));
        converters.put(Long.class, s -> Long.valueOf(s));

        converters.put(float.class, s -> Float.parseFloat(s));
        converters.put(Float.class, s -> Float.valueOf(s));

        converters.put(double.class, s -> Double.parseDouble(s));
        converters.put(Double.class, s -> Double.valueOf(s));

        converters.put(LocalDate.class, s -> LocalDate.parse(s));
        converters.put(LocalTime.class, s -> LocalTime.parse(s));
        converters.put(LocalDateTime.class, s -> LocalDateTime.parse(s));
        converters.put(ZonedDateTime.class, s -> ZonedDateTime.parse(s));
        converters.put(Duration.class, s -> Duration.parse(s));
        converters.put(ZoneId.class, s -> ZoneId.of(s));
    }

    @Nullable
    public String getProperty(String key) {
        return getProperty(key, String.class);
    }

    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null ? parseValue(defaultValue) : value;
    }

    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        T value = getProperty(key, targetType);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    @Nullable
    public <T> T getProperty(String key, Class<T> targetType) {
        PropertyExpr keyExpr = parsePropertyExpr(key);
        String value;
        if (keyExpr != null) {
            if (keyExpr.defaultValue() != null) {
                value = getProperty(keyExpr.key(), keyExpr.defaultValue());
                return convert(value, targetType);
            } else {
                return getProperty(keyExpr.key(), targetType);
            }
        }

        value = this.properties.get(key);

        if (value == null) {
            return null;
        }

        return convert(value, targetType);
    }

    public String getRequiredProperty(String key) {
        String value = getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Required key '" + key + "' not found");
        }
        return value;
    }

    public <T> T getRequiredProperty(String key, Class<T> targetType) {
        T value = getProperty(key, targetType);
        if (value == null) {
            throw new IllegalStateException("Required key '" + key + "' not found");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private <T> T convert(String value, Class<T> clazz) {
        Function<String, Object> function = this.converters.get(clazz);
        if (function == null) {
            throw new IllegalArgumentException("Unsupported value type: " + clazz.getName());
        }
        return (T) function.apply(value);
    }

    private String parseValue(String value) {
        PropertyExpr propertyExpr = parsePropertyExpr(value);
        if (propertyExpr == null) {
            return value;
        }
        if (propertyExpr.defaultValue() != null) {
            return getProperty(propertyExpr.key(), propertyExpr.defaultValue());
        } else {
            return getProperty(propertyExpr.key());
        }
    }

    private PropertyExpr parsePropertyExpr(String key) {
        if (key.startsWith("${") && key.endsWith("}")) {
            int n = key.indexOf(":");
            if (n == -1) {
                String k = key.substring(2, key.length() - 1);
                return new PropertyExpr(k, null);
            } else {
                String k = key.substring(2, n);
                return new PropertyExpr(k, key.substring(n + 1, key.length() - 1));
            }
        }
        return null;
    }
}


record PropertyExpr(String key, String defaultValue) {
}