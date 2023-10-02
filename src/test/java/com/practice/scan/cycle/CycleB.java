package com.practice.scan.cycle;

import com.practice.diy.annotation.Autowired;
import com.practice.diy.annotation.Component;

@Component
public class CycleB {
    @Autowired
    CycleA cycleA;

    public CycleA getCycleA() {
        return cycleA;
    }
}
