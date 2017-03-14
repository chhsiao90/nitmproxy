package com.github.chhsiao.nitm.nitmproxy;

import java.util.Arrays;
import java.util.List;

public class NitmProxyConfig {
    private ProxyMode proxyMode;

    // TLS related
    private List<Integer> httpsPorts;
    private String certFile;
    private String keyFile;

    private int maxContentLength;

    private boolean clientHttp2;
    private boolean serverHttp2;

    public NitmProxyConfig() {
        // Defaults
        proxyMode = ProxyMode.HTTP;

        httpsPorts = Arrays.asList(443, 8443);
        certFile = "server.pem";
        keyFile = "key.pem";

        maxContentLength = 1024 * 1024;

        clientHttp2 = true;
        serverHttp2 = true;
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

    public boolean isClientHttp2() {
        return clientHttp2;
    }

    public void setClientHttp2(boolean clientHttp2) {
        this.clientHttp2 = clientHttp2;
    }

    public boolean isServerHttp2() {
        return serverHttp2;
    }

    public void setServerHttp2(boolean serverHttp2) {
        this.serverHttp2 = serverHttp2;
    }
}