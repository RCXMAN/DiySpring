package com.practice.diy.io;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class PropertyResolverTest {
    @Test
    public void propertyValue() {
        Properties properties = new Properties();
        properties.setProperty("app.title", "Summer Framework");
        properties.setProperty("app.version", "v1.0");
        properties.setProperty("jdbc.url", "jdbc:mysql://localhost:3306/simpsons");
        properties.setProperty("jdbc.username", "bart");
        properties.setProperty("jdbc.password", "51mp50n");
        properties.setProperty("jdbc.pool-size", "20");
        properties.setProperty("jdbc.auto-commit", "true");
        properties.setProperty("scheduler.started-at", "2023-03-29T21:45:01");
        properties.setProperty("scheduler.backup-at", "03:05:10");
        properties.setProperty("scheduler.cleanup", "P2DT8H21M");
        
        PropertyResolver propertyResolver = new PropertyResolver(properties);
        assertEquals("Summer Framework", propertyResolver.getProperty("app.title"));
        assertEquals("v1.0", propertyResolver.getProperty("app.version"));
        assertEquals("v1.0", propertyResolver.getProperty("app.version", "unknown"));
        assertNull(propertyResolver.getProperty("app.author"));
        assertEquals("Michael Liao", propertyResolver.getProperty("app.author", "Michael Liao"));

        assertTrue(propertyResolver.getProperty("jdbc.auto-commit", boolean.class));
        assertEquals(Boolean.TRUE, propertyResolver.getProperty("jdbc.auto-commit", Boolean.class));
        assertTrue(propertyResolver.getProperty("jdbc.detect-leak", boolean.class, true));

        assertEquals(20, propertyResolver.getProperty("jdbc.pool-size", int.class));
        assertEquals(20, propertyResolver.getProperty("jdbc.pool-size", int.class, 999));
        assertEquals(5, propertyResolver.getProperty("jdbc.idle", int.class, 5));

        assertEquals(LocalDateTime.parse("2023-03-29T21:45:01"), propertyResolver.getProperty("scheduler.started-at", LocalDateTime.class));
        assertEquals(LocalTime.parse("03:05:10"), propertyResolver.getProperty("scheduler.backup-at", LocalTime.class));
        assertEquals(LocalTime.parse("23:59:59"), propertyResolver.getProperty("scheduler.restart-at", LocalTime.class, LocalTime.parse("23:59:59")));
        assertEquals(Duration.ofMinutes((2 * 24 + 8) * 60 + 21), propertyResolver.getProperty("scheduler.cleanup", Duration.class));
    }

    @Test
    public void requiredProperty() {
        Properties props = new Properties();
        props.setProperty("app.title", "DIY Framework");
        props.setProperty("app.id", "10");

        PropertyResolver propertyResolver = new PropertyResolver(props);
        assertEquals("DIY Framework", propertyResolver.getRequiredProperty("app.title"));
        assertThrows(IllegalStateException.class, () -> {
            propertyResolver.getRequiredProperty("not.exist");
        });

        assertEquals(10, propertyResolver.getRequiredProperty("app.id", int.class));
        assertThrows(IllegalStateException.class, () -> {
            propertyResolver.getRequiredProperty("app.idd");
        });
    }

    @Test
    public void propertyHolder() {
        String home = "/Users/cixiang";

        Properties props = new Properties();
        props.setProperty("app.title", "DIY Framework");

        PropertyResolver propertyResolver = new PropertyResolver(props);
        assertEquals("DIY Framework", propertyResolver.getProperty("${app.title}"));
        assertEquals(null,
            propertyResolver.getProperty("${app.version}"));
        assertEquals("v1.0", propertyResolver.getProperty("${app.version:v1.0}"));
        assertEquals(1, propertyResolver.getProperty("${app.version:1}", int.class));
        assertThrows(NumberFormatException.class, () -> {
            propertyResolver.getProperty("${app.version:x}", int.class);
        });

        assertEquals(home, propertyResolver.getProperty("${app.path:${HOME}}"));
        assertEquals(home, propertyResolver.getProperty("${app.path:${app.home:${HOME}}}"));
        assertEquals("/not-exist", propertyResolver.getProperty("${app.path:${app.home:${ENV_NOT_EXIST:/not-exist}}}"));
    }

}
