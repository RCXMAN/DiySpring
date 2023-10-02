package com.practice.imported;

import com.practice.diy.annotation.Bean;
import com.practice.diy.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
public class LocalDateConfiguration {
    @Bean
    LocalDate startLocalDate() {
        return LocalDate.now();
    }

    @Bean
    LocalDateTime startLocalDateTime() {
        return LocalDateTime.now();
    }
}
