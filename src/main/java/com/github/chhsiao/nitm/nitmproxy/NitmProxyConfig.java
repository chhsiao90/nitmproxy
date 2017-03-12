package com.github.chhsiao.nitm.nitmproxy;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;

public class NitmProxyConfig {
    private ProxyMode proxyMode;

    private List<Integer> httpsPorts;
    private SslContext clientSslCtx;
    private SslContext serverSslCtx;

    private int maxContentLength;

    public NitmProxyConfig() {
        // Defaults
        proxyMode = ProxyMode.HTTP;

        httpsPorts = Arrays.asList(443, 8443);

        try {
            clientSslCtx = SslContextBuilder.forClient().build();

            SelfSignedCertificate ssc = new SelfSignedCertificate();
            serverSslCtx = SslContextBuilder
                    .forServer(ssc.certificate(), ssc.privateKey())
                    .build();
        } catch (SSLException | CertificateException e) {
            throw new IllegalStateException(e);
        }

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

    public SslContext getClientSslCtx() {
        return clientSslCtx;
    }

    public void setClientSslCtx(SslContext clientSslCtx) {
        this.clientSslCtx = clientSslCtx;
    }

    public SslContext getServerSslCtx() {
        return serverSslCtx;
    }

    public void setServerSslCtx(SslContext serverSslCtx) {
        this.serverSslCtx = serverSslCtx;
    }

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }
}