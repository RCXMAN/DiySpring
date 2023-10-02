package com.practice.scan.cycle;

import com.practice.diy.annotation.Autowired;
import com.practice.diy.annotation.Component;

@Component
public class CycleA {
    @Autowired
    CycleB cycleB;

    public CycleB getCycleB() {
        return cycleB;
    }
}
