package com.practice.imported;


import com.practice.diy.annotation.Bean;
import com.practice.diy.annotation.Configuration;

import java.time.ZonedDateTime;

@Configuration
public class ZonedDateConfiguration {

    @Bean
    ZonedDateTime startZonedDateTime() {
        return ZonedDateTime.now();
    }
}