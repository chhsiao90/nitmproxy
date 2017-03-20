package com.github.chhsiaoninety.nitmproxy;

import com.github.chhsiaoninety.nitmproxy.enums.ProxyMode;
import com.google.common.base.Joiner;

import java.util.Arrays;
import java.util.List;

public class NitmProxyConfig {
    private ProxyMode proxyMode;

    private String host;
    private int port;

    // TLS related
    private List<Integer> httpsPorts;
    private String certFile;
    private String keyFile;
    private boolean insecure;

    private int maxContentLength;

    private boolean clientHttp2;
    private boolean serverHttp2;

    // Default values
    public NitmProxyConfig() {
        proxyMode = ProxyMode.HTTP;

        host = "127.0.0.1";
        port = 8080;

        httpsPorts = Arrays.asList(443, 8443);
        certFile = "server.pem";
        keyFile = "key.pem";
        insecure = false;

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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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

    public boolean isInsecure() {
        return insecure;
    }

    public void setInsecure(boolean insecure) {
        this.insecure = insecure;
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

    @Override
    public String toString() {
        List<String> properties = Arrays.asList(
                String.format("proxyMode=%s", proxyMode),
                String.format("host=%s", host),
                String.format("port=%s", port),
                String.format("httpsPorts=%s", httpsPorts),
                String.format("certFile=%s", certFile),
                String.format("keyFile=%s", keyFile),
                String.format("insecure=%b", insecure),
                String.format("maxContentLength=%d", maxContentLength),
                String.format("clientHttp2=%s", clientHttp2),
                String.format("serverHttp2=%s", serverHttp2));
        return String.format("NitmProxyConfig%n%s",
                             Joiner.on(System.lineSeparator()).join(properties));
    }
}