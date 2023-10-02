package com.practice.scan.sub1.sub2;

import com.practice.diy.annotation.Autowired;
import com.practice.diy.annotation.Component;
import com.practice.scan.sub1.sub2.sub3.Sub3Bean;

@Component
public class Sub2Bean {
    @Autowired
    Sub3Bean sub3Bean;


}
