package com.github.chhsiao.nitm.nitmproxy;

import java.util.Arrays;
import java.util.List;

public class NitmProxyConfig {
    private ProxyMode proxyMode;

    private List<Integer> httpsPorts;

    private int maxContentLength;

    public NitmProxyConfig() {
        // Defaults
        proxyMode = ProxyMode.HTTP;

        httpsPorts = Arrays.asList(443, 8443);

        maxContentLength = 4096;
    }

    public ProxyMode getProxyMode() {
        return proxyMode;
    }

    public void setProxyMode(ProxyMode proxyMode) {
        this.proxyMode = proxyMode;
    }

    public List<Integer> getHttpsPorts() {
        return httpsPorts;
    }

    public void setHttpsPorts(List<Integer> httpsPorts) {
        this.httpsPorts = httpsPorts;
    }

    public boolean isTls(int port) {
        return httpsPorts.contains(port);
    }

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }
}