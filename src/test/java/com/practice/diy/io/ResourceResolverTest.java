package com.practice.diy.io;


import com.practice.diy.io.ResourceResolver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.sub.ScanAnno;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceResolverTest {

    @Test
    public void scan() throws IOException {
        String pkg = "com.practice.scan";
        ResourceResolver resourceResolver = new ResourceResolver(pkg);
        List<String> classes = resourceResolver.scan(resource -> {
            String name = resource.name();
            if (name.endsWith(".class")) {
                return name.substring(0, name.length() - 6)
                        .replace("/", ".")
                        .replace("\\", ".");
            }
            return null;
        });

        Collections.sort(classes);
        System.out.println(classes);
        String[] listOfClasses = new String[] {
                "com.practice.scan.sub1.Sub1Bean",
                "com.practice.scan.sub1.sub2.Sub2Bean",
                "com.practice.scan.sub1.sub2.sub3.Sub3Bean",
        };

        for (String clazz : listOfClasses) {
            assertTrue(classes.contains(clazz));
        }
    }

    @Test
    public void scanJar() throws IOException {
        String pkg = PostConstruct.class.getPackageName();
        ResourceResolver resourceResolver = new ResourceResolver(pkg);
        List<String> classes = resourceResolver.scan(resource -> {
            String name = resource.name();
            if (name.endsWith(".class")) {
                return name.substring(0, name.length() - 6)
                        .replace("/", ".")
                        .replace("\\", ".");
            }
            return null;
        });

        assertTrue(classes.contains(PostConstruct.class.getName()));
        assertTrue(classes.contains(PreDestroy.class.getName()));
        assertTrue(classes.contains(PermitAll.class.getName()));
        assertTrue(classes.contains(ScanAnno.class.getName()));
    }

    @Test
    public void scanTxt() throws IOException {
        String pkg = "com.practice.scan";
        ResourceResolver resourceResolver = new ResourceResolver(pkg);
        List<String> classes = resourceResolver.scan(resource -> {
            String name = resource.name();
            if (name.endsWith(".txt")) {
                return name.replace("\\", "/");
            }
            return null;
        });
        Collections.sort(classes);
        assertArrayEquals(new String[] {
                // txt files:
                "com/practice/scan/sub1/sub1.txt", //
                "com/practice/scan/sub1/sub2/sub2.txt", //
                "com/practice/scan/sub1/sub2/sub3/sub3.txt", //
        }, classes.toArray(String[]::new));
    }

}
