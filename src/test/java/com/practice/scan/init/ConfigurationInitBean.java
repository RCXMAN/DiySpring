package com.practice.scan.init;

import com.practice.diy.annotation.Bean;
import com.practice.diy.annotation.Configuration;
import com.practice.diy.annotation.Value;

@Configuration
public class ConfigurationInitBean {
    @Bean(initMethod = "init")
    ConfigInitHelpBean createSpecifyInitBean(@Value("${app.title}") String appTitle, @Value("${app.version}") String appVersion) {
        return new ConfigInitHelpBean(appTitle, appVersion);
    }
}
