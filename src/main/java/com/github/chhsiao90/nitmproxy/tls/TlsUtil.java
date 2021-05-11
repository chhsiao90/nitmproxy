package com.github.chhsiao90.nitmproxy.tls;

import static io.netty.handler.ssl.ApplicationProtocolNames.HTTP_1_1;
import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.TlsContext;

import java.io.File;
import java.security.KeyStore;
import java.util.List;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TlsUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(TlsUtil.class);

    private static final TrustManagerFactory TRUST_MANAGER_FACTORY;

    static {
        TrustManagerFactory trustManagerFactory = null;
        try {
            trustManagerFactory = TrustManagerFactory.getInstance(getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
        } catch (Exception ignore) {
            // ignore
        }
        TRUST_MANAGER_FACTORY = trustManagerFactory;
    }

    private TlsUtil() {
    }

    public static SslContext ctxForClient(ConnectionContext context) throws SSLException {
        SslContextBuilder builder = SslContextBuilder
                .forClient()
                .protocols(context.config().getTlsProtocols())
                .sslContextProvider(context.config().getSslProvider())
                .applicationProtocolConfig(applicationProtocolConfig(context.tlsCtx()));
        if (context.config().getClientKeyManagerFactory() != null) {
            builder.keyManager(context.config().getClientKeyManagerFactory());
        }
        if (context.config().getTrustManager() != null) {
            builder.trustManager(context.config().getTrustManager());
        }
        if (context.config().isInsecure()) {
            builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        } else if (TRUST_MANAGER_FACTORY != null && context.config().getTrustManager() == null) {
            builder.trustManager(TRUST_MANAGER_FACTORY);
        }
        return builder.build();
    }

    public static SslContext ctxForServer(ConnectionContext context) throws SSLException {
        String certFile = new File(context.config().getCertFile()).getAbsolutePath();
        String keyFile = new File(context.config().getKeyFile()).getAbsolutePath();

        LOGGER.debug("ABBAS CERTS: {}, {}, {}", certFile, keyFile, context.getServerAddr().getHost());

        Certificate certificate = CertUtil.newCert(
                certFile, keyFile, context.getServerAddr().getHost());

        LOGGER.debug("ABBAS SSL: {}, {}, {}, {}",
                context.getServerAddr().getHost(),
                context.config().getTlsProtocols(),
                context.config().getSslProvider(),
                alpnProtocols(context.tlsCtx()));

        return SslContextBuilder
                .forServer(certificate.getKeyPair().getPrivate(), certificate.getChain())
                .protocols(context.config().getTlsProtocols())
                .sslContextProvider(context.config().getSslProvider())
                .applicationProtocolConfig(applicationProtocolConfig(context.tlsCtx()))
                .build();
    }

    private static ApplicationProtocolConfig applicationProtocolConfig(TlsContext tlsContext) {
        return new ApplicationProtocolConfig(
                Protocol.ALPN,
                SelectorFailureBehavior.NO_ADVERTISE,
                SelectedListenerFailureBehavior.ACCEPT,
                alpnProtocols(tlsContext));
    }

    private static String[] alpnProtocols(TlsContext tlsCtx) {
        if (tlsCtx.isNegotiated()) {
            return new String[] { tlsCtx.protocol() };
        }
        if (tlsCtx.protocolsPromise().isDone()) {
            List<String> protocols = tlsCtx.protocols();
            if (protocols != null && !protocols.isEmpty()) {
                return protocols.toArray(new String[0]);
            }
        }
        return new String[] { HTTP_1_1 };
    }
}
