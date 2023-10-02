package com.practice.scan.nest;

import com.practice.diy.annotation.Component;

@Component
public class NestBean {
    @Component
    public static class InnerBean {

    }
}
