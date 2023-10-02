package com.practice.scan.primary;

import com.practice.diy.annotation.Bean;
import com.practice.diy.annotation.Configuration;
import com.practice.diy.annotation.Primary;

@Configuration
public class PrimaryConfiguration {

    @Primary
    @Bean
    DogBean husky() {
        return new DogBean("Husky");
    }

    @Bean
    DogBean teddy() {
        return new DogBean("Teddy");
    }
}
