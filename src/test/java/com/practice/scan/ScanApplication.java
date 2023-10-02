package com.practice.scan;

import com.practice.diy.annotation.ComponentScan;
import com.practice.diy.annotation.Import;
import com.practice.imported.LocalDateConfiguration;
import com.practice.imported.ZonedDateConfiguration;

@ComponentScan
@Import({ LocalDateConfiguration.class, ZonedDateConfiguration.class })
public class ScanApplication {
}
