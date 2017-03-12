package com.github.chhsiao.nitm.nitmproxy;

public class NitmProxyConfig {
    private ProxyMode proxyMode;
    private int maxContentLength;

    public ProxyMode getProxyMode() {
        return proxyMode;
    }

    public void setProxyMode(ProxyMode proxyMode) {
        this.proxyMode = proxyMode;
    }

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }
}