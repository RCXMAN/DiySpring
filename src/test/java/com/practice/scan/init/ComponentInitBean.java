package com.practice.scan.init;


import com.practice.diy.annotation.Component;
import com.practice.diy.annotation.Value;
import jakarta.annotation.PostConstruct;

@Component
public class ComponentInitBean {
    @Value("${app.title}")
    String appTitle;

    @Value("${app.version}")
    String appVersion;

    public String appName;

    @PostConstruct
    void init() {
        this.appName = this.appTitle + " / " + this.appVersion;
    }
}
