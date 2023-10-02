package com.practice.scan.destroy;

import com.practice.diy.annotation.Component;
import com.practice.diy.annotation.Value;
import jakarta.annotation.PreDestroy;

@Component
public class AnnotationDestroyBean {
    @Value("${app.title}")
    public String appTitle;

    @PreDestroy
    void destroy() {
        this.appTitle = null;
    }
}
