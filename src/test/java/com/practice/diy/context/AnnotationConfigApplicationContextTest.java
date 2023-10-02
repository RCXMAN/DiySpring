package com.practice.diy.context;

import com.practice.diy.io.PropertyResolver;
import com.practice.imported.LocalDateConfiguration;
import com.practice.imported.ZonedDateConfiguration;
import com.practice.scan.ScanApplication;
import com.practice.scan.cycle.CycleA;
import com.practice.scan.cycle.CycleB;
import com.practice.scan.destroy.AnnotationDestroyBean;
import com.practice.scan.destroy.SpecifyDestroyBean;
import com.practice.scan.init.ComponentInitBean;
import com.practice.scan.init.ConfigInitHelpBean;
import com.practice.scan.mutiple.EmailNotificationService;
import com.practice.scan.mutiple.NotificationManager;
import com.practice.scan.mutiple.NotificationService;
import com.practice.scan.nest.NestBean;
import com.practice.scan.primary.DogBean;
import com.practice.scan.primary.PersonBean;
import com.practice.scan.primary.TeacherBean;
import com.practice.scan.sub1.Sub1Bean;
import com.practice.scan.sub1.sub2.Sub2Bean;
import com.practice.scan.sub1.sub2.sub3.Sub3Bean;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AnnotationConfigApplicationContextTest {
    @Test
    public void testImport() throws IOException {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(createPropertyResolver(), ScanApplication.class)) {
            assertNotNull(ctx.getBean(LocalDateConfiguration.class));
            assertNotNull(ctx.getBean("startLocalDate"));
            assertNotNull(ctx.getBean("startLocalDateTime"));
            assertNotNull(ctx.getBean(ZonedDateConfiguration.class));
            assertNotNull(ctx.getBean("startZonedDateTime"));
        }
    }

    @Test
    public void testInitMethod() throws IOException {
        // test @PostConstruct:
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(createPropertyResolver(), ScanApplication.class)) {
            ComponentInitBean bean1 = ctx.getBean(ComponentInitBean.class);
            ConfigInitHelpBean bean2 = ctx.getBean(ConfigInitHelpBean.class);
            assertEquals("Scan App / v1.0", bean1.appName);
            assertEquals("Scan App / v1.0", bean2.appName);
        }
    }

    @Test
    public void testDestroyMethod() throws IOException {
        AnnotationDestroyBean bean1 = null;
        SpecifyDestroyBean bean2 = null;
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(createPropertyResolver(), ScanApplication.class)){
            bean1 = ctx.getBean(AnnotationDestroyBean.class);
            bean2 = ctx.getBean(SpecifyDestroyBean.class);
            assertEquals("Scan App", bean1.appTitle);
            assertEquals("Scan App", bean2.appTitle);
        }
        assertNull(bean1.appTitle);
        assertNull(bean2.appTitle);
    }

    @Test
    public void testNested() throws IOException {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(createPropertyResolver(), ScanApplication.class)) {
            ctx.getBean(NestBean.class);
            ctx.getBean(NestBean.InnerBean.class);
        }
    }

    @Test
    public void testPrimary() throws IOException {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(createPropertyResolver(), ScanApplication.class)) {
            PersonBean person = ctx.getBean(PersonBean.class);
            assertEquals(TeacherBean.class, person.getClass());
            DogBean dog = ctx.getBean(DogBean.class);
            assertEquals("Husky", dog.type);
        }
    }

    @Test
    public void testSub() throws IOException {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(createPropertyResolver(), ScanApplication.class)) {
            ctx.getBean(Sub1Bean.class);
            ctx.getBean(Sub2Bean.class);
            ctx.getBean(Sub3Bean.class);
        }
    }

    @Test
    public void testMultiple() throws IOException {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(createPropertyResolver(), ScanApplication.class)) {
            ctx.getBean(NotificationManager.class);
            List<NotificationService> beans = ctx.getBeans(NotificationService.class);
            NotificationService emailService = ctx.getBean("emailService");
            NotificationService smsService = ctx.getBean("smsService");
            assertEquals(List.of(emailService, smsService), beans);
            assertEquals(emailService, ctx.getBean("emailService", EmailNotificationService.class));
        }
    }

    @Test
    public void testCycle() throws IOException {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(createPropertyResolver(), ScanApplication.class)) {
            CycleA cycleA = ctx.getBean(CycleA.class);
            CycleB cycleB = ctx.getBean(CycleB.class);
            assertEquals(CycleB.class, cycleA.getCycleB().getClass());
            assertEquals(CycleA.class, cycleB.getCycleA().getClass());
        }
    }

    @Test
    public void testUtils() throws IOException {
        assertThrows(NullPointerException.class, () -> ApplicationContextUtils.getRequiredApplicationContext());
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(createPropertyResolver(), ScanApplication.class)) {
            assertEquals(ctx, ApplicationContextUtils.getApplicationContext());
        }
    }


    PropertyResolver createPropertyResolver() {
        Properties properties = new Properties();
        properties.put("app.title", "Scan App");
        properties.put("app.version", "v1.0");
        properties.put("jdbc.url", "jdbc:hsqldb:file:testdb.tmp");
        properties.put("jdbc.username", "sa");
        properties.put("jdbc.password", "");
        properties.put("convert.boolean", "true");
        properties.put("convert.byte", "123");
        properties.put("convert.short", "12345");
        properties.put("convert.integer", "1234567");
        properties.put("convert.long", "123456789000");
        properties.put("convert.float", "12345.6789");
        properties.put("convert.double", "123456789.87654321");
        properties.put("convert.localdate", "2023-03-29");
        properties.put("convert.localtime", "20:45:01");
        properties.put("convert.localdatetime", "2023-03-29T20:45:01");
        properties.put("convert.zoneddatetime", "2023-03-29T20:45:01+08:00[Asia/Shanghai]");
        properties.put("convert.duration", "P2DT3H4M");
        properties.put("convert.zoneid", "Asia/Shanghai");
        PropertyResolver propertyResolver = new PropertyResolver(properties);
        return propertyResolver;
    }
}
