package com.practice.scan.sub1;

import com.practice.diy.annotation.Autowired;
import com.practice.diy.annotation.Component;
import com.practice.scan.sub1.sub2.Sub2Bean;
import com.practice.scan.sub1.sub2.sub3.Sub3Bean;

@Component
public class Sub1Bean {
    Sub2Bean sub2Bean;

    public Sub1Bean(@Autowired Sub2Bean sub2Bean) {
        this.sub2Bean = sub2Bean;
    }
}
