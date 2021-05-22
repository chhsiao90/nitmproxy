package com.github.chhsiao90.nitmproxy;

import com.github.chhsiao90.nitmproxy.enums.ProxyMode;
import com.github.chhsiao90.nitmproxy.handler.protocol.ProtocolDetector;
import com.github.chhsiao90.nitmproxy.handler.protocol.http1.Http1ProtocolDetector;
import com.github.chhsiao90.nitmproxy.listener.HttpListener;
import com.google.common.base.Joiner;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.String.*;
import static java.lang.System.*;
import static java.util.Arrays.*;

public class NitmProxyConfig {

    private ProxyMode proxyMode;

    private String host;
    private int port;

    // TLS related
    private String certFile;
    private String keyFile;
    private boolean insecure;
    private Provider sslProvider;
    private List<String> tlsProtocols;
    private KeyManagerFactory clientKeyManagerFactory;

    private int maxContentLength;

    private NitmProxyStatusListener statusListener;
    private List<HttpListener> httpListeners;
    private TrustManager trustManager;

    private List<ProtocolDetector> detectors;

    // Default values
    public NitmProxyConfig() {
        proxyMode = ProxyMode.HTTP;

        host = "127.0.0.1";
        port = 8080;

        certFile = "server.pem";
        keyFile = "key.pem";
        insecure = false;
        tlsProtocols = asList("TLSv1.3", "TLSv1.2");

        maxContentLength = 1024 * 1024;

        httpListeners = new ArrayList<>();
        detectors = Collections.singletonList(Http1ProtocolDetector.INSTANCE);
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

    public Provider getSslProvider() {
        return sslProvider;
    }

    public void setSslProvider(Provider sslProvider) {
        this.sslProvider = sslProvider;
    }

    public List<String> getTlsProtocols() {
        return tlsProtocols;
    }

    public void setTlsProtocols(List<String> tlsProtocols) {
        this.tlsProtocols = tlsProtocols;
    }

    public KeyManagerFactory getClientKeyManagerFactory() {
        return clientKeyManagerFactory;
    }

    public void setClientKeyManagerFactory(KeyManagerFactory clientKeyManagerFactory) {
        this.clientKeyManagerFactory = clientKeyManagerFactory;
    }

    public TrustManager getTrustManager() {
        return trustManager;
    }

    public void setTrustManager(TrustManager trustManager) {
        this.trustManager = trustManager;
    }

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    public List<HttpListener> getHttpListeners() {
        return httpListeners;
    }

    public void setHttpListeners(List<HttpListener> httpListeners) {
        this.httpListeners = httpListeners;
    }

    public List<ProtocolDetector> getDetectors() {
        return detectors;
    }

    public void setDetectors(List<ProtocolDetector> detectors) {
        this.detectors = detectors;
    }

    public NitmProxyStatusListener getStatusListener() {
        return statusListener;
    }

    public void setStatusListener(NitmProxyStatusListener statusListener) {
        this.statusListener = statusListener;
    }

    @Override
    public String toString() {
        List<String> properties = asList(
                format("proxyMode=%s", proxyMode),
                format("host=%s", host),
                format("port=%s", port),
                format("certFile=%s", certFile),
                format("keyFile=%s", keyFile),
                format("insecure=%b", insecure),
                format("tlsProtocols=%s", tlsProtocols),
                format("sslProvider=%s", sslProvider),
                format("keyManagerFactory=%b", clientKeyManagerFactory),
                format("maxContentLength=%d", maxContentLength));
        return format("NitmProxyConfig%n%s", Joiner.on(lineSeparator()).join(properties));
    }
}