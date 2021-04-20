package com.github.chhsiao90.nitmproxy;

import com.github.chhsiao90.nitmproxy.enums.ProxyMode;
import com.google.common.base.Joiner;

import javax.net.ssl.KeyManagerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class NitmProxyConfig {
    private ProxyMode proxyMode;

    private String host;
    private int port;

    // TLS related
    private List<Integer> httpsPorts;
    private String certFile;
    private String keyFile;
    private boolean insecure;
    private KeyManagerFactory clientKeyManagerFactory;

    private int maxContentLength;

    private Consumer<byte[]> requestLogger;
    private Consumer<byte[]> responseLogger;

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

    public KeyManagerFactory getClientKeyManagerFactory() {
        return clientKeyManagerFactory;
    }

    public void setClientKeyManagerFactory(KeyManagerFactory clientKeyManagerFactory) {
        this.clientKeyManagerFactory = clientKeyManagerFactory;
    }

    public Consumer<byte[]> getRequestLogger() {
        return requestLogger;
    }

    public void setRequestLogger(Consumer<byte[]> requestLogger) {
        this.requestLogger = requestLogger;
    }

    public Consumer<byte[]> getResponseLogger() {
        return responseLogger;
    }

    public void setResponseLogger(Consumer<byte[]> responseLogger) {
        this.responseLogger = responseLogger;
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
                String.format("keyManagerFactory=%b", clientKeyManagerFactory),
                String.format("maxContentLength=%d", maxContentLength));
        return String.format("NitmProxyConfig%n%s",
                             Joiner.on(System.lineSeparator()).join(properties));
    }
}