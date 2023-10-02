package com.practice.scan.destroy;

import com.practice.diy.annotation.Bean;
import com.practice.diy.annotation.Configuration;
import com.practice.diy.annotation.Value;

@Configuration
public class SpecifyDestroyConfiguration {

    @Bean(destroyMethod = "destroy")
    SpecifyDestroyBean createSpecifyDestroyBean(@Value("${app.title}") String appTitle) {
        return new SpecifyDestroyBean(appTitle);
    }
}
