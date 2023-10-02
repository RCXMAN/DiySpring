package com.practice.scan.init;

public class ConfigInitHelpBean {

    String appTitle;

    String appVersion;

    public String appName;

    ConfigInitHelpBean(String appTitle, String appVersion) {
        this.appTitle = appTitle;
        this.appVersion = appVersion;
    }

    void init() {
        this.appName = this.appTitle + " / " + this.appVersion;
    }
}
