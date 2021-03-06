package com.appdiscovery.app;

import com.appdiscovery.app.services.LanServerAvailabilityMonitor;

public class Config {
    private static final Config ourInstance = new Config();

    public static Config getInstance() {
        return ourInstance;
    }

    public String centralServerAddr = "http://ad-central-server.kevinwang.cc:889";

    public String repoServerAddr = "http://ad-app-repo-dynamic.kevinwang.cc:888";
    public String canonicalRepoServerAddr = "http://ad-central-server.kevinwang.cc:888";
    public String lanRepoServerAddr = "http://lan-app-repo-server.appd:888";

    public static String getRepoServerAddr(boolean bypassLan) {
        if (LanServerAvailabilityMonitor.lanAvailable && !bypassLan) {
            return getInstance().lanRepoServerAddr;
        } else {
            return getInstance().repoServerAddr;
        }
    }

    public static String getRepoServerAddr() {
        return getRepoServerAddr(false);
    }

    private Config() {
    }
}
