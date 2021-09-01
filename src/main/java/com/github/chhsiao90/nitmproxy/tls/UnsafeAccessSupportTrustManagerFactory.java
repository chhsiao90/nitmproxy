package com.github.chhsiao90.nitmproxy.tls;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import io.netty.handler.ssl.util.SimpleTrustManagerFactory;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class UnsafeAccessSupportTrustManagerFactory extends SimpleTrustManagerFactory {

    private final TrustManager tm;

    public UnsafeAccessSupportTrustManagerFactory(
            X509TrustManager delegate,
            UnsafeAccessSupport unsafeAccessSupport,
            ConnectionContext context) {
        this.tm = new UnsafeAccessSupportTrustManager(delegate, unsafeAccessSupport, context);
    }

    public static TrustManagerFactory create(
            TrustManagerFactory factory,
            UnsafeAccessSupport unsafeAccessSupport,
            ConnectionContext context) {
        return Arrays.stream(factory.getTrustManagers())
                .filter(X509TrustManager.class::isInstance)
                .map(X509TrustManager.class::cast)
                .findFirst()
                .<TrustManagerFactory>map(tm -> new UnsafeAccessSupportTrustManagerFactory(
                        tm, unsafeAccessSupport, context))
                .orElse(factory);
    }

    @Override
    protected void engineInit(KeyStore keyStore) {
    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) {
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        return new TrustManager[] { tm };
    }

    private static class UnsafeAccessSupportTrustManager implements X509TrustManager {

        private final X509TrustManager delegate;
        private final UnsafeAccessSupport unsafeAccessSupport;
        private final ConnectionContext context;

        public UnsafeAccessSupportTrustManager(
                X509TrustManager delegate,
                UnsafeAccessSupport unsafeAccessSupport,
                ConnectionContext context) {
            this.delegate = delegate;
            this.unsafeAccessSupport = unsafeAccessSupport;
            this.context = context;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                delegate.checkClientTrusted(chain, authType);
            } catch (CertificateException e) {
                switch (unsafeAccessSupport.checkUnsafeAccess(context, chain, e)) {
                    case ACCEPT:
                        break;
                    case DENY:
                        throw e;
                    case ASK:
                        context.tlsCtx().askUnsafeAccess();
                }
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                delegate.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                switch (unsafeAccessSupport.checkUnsafeAccess(context, chain, e)) {
                    case ACCEPT:
                        break;
                    case DENY:
                        throw e;
                }
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }
    }
}
