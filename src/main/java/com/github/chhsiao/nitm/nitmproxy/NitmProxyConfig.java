package com.github.chhsiao.nitm.nitmproxy;

import java.util.Arrays;
import java.util.List;

public class NitmProxyConfig {
    private ProxyMode proxyMode;

    private List<Integer> httpsPorts;

    private String certFile;

    private String keyFile;

    private int maxContentLength;

    public NitmProxyConfig() {
        // Defaults
        proxyMode = ProxyMode.HTTP;

        httpsPorts = Arrays.asList(443, 8443);
        certFile = "server.pem";
        keyFile = "key.pem";

        maxContentLength = 1024 * 1024;
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

    public String getCertFile() {
        return certFile;
    }

    public void setCertFile(String certFile) {
        this.certFile = certFile;
    }

    public String getKeyFile() {
        return keyFile;
    }

    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
    }

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }
}