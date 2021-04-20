package com.github.chhsiao90.nitmproxy;

import com.github.chhsiao90.nitmproxy.enums.ProxyMode;
import com.google.common.base.Joiner;

import javax.net.ssl.KeyManagerFactory;
import java.util.Arrays;
import java.util.List;

public class NitmProxyConfig {
    private ProxyMode proxyMode;

    private String host;
    private int port;

    // TLS related
    private String certFile;
    private String keyFile;
    private boolean insecure;
    private KeyManagerFactory clientKeyManagerFactory;

    private int maxContentLength;

    // Default values
    public NitmProxyConfig() {
        proxyMode = ProxyMode.HTTP;

        host = "127.0.0.1";
        port = 8080;

        certFile = "server.pem";
        keyFile = "key.pem";
        insecure = false;

        maxContentLength = 1024 * 1024;
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

    public KeyManagerFactory getClientKeyManagerFactory() {
        return clientKeyManagerFactory;
    }

    public void setClientKeyManagerFactory(KeyManagerFactory clientKeyManagerFactory) {
        this.clientKeyManagerFactory = clientKeyManagerFactory;
    }

    @Override
    public String toString() {
        List<String> properties = Arrays.asList(
                String.format("proxyMode=%s", proxyMode),
                String.format("host=%s", host),
                String.format("port=%s", port),
                String.format("certFile=%s", certFile),
                String.format("keyFile=%s", keyFile),
                String.format("insecure=%b", insecure),
                String.format("keyManagerFactory=%b", clientKeyManagerFactory),
                String.format("maxContentLength=%d", maxContentLength));
        return String.format("NitmProxyConfig%n%s",
                             Joiner.on(System.lineSeparator()).join(properties));
    }
}