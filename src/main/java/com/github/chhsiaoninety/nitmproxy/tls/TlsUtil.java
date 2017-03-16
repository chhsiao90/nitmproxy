package com.github.chhsiaoninety.nitmproxy.tls;

import com.github.chhsiaoninety.nitmproxy.NitmProxyConfig;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;

public class TlsUtil {
    public static SslContext ctxForClient(NitmProxyConfig config) throws SSLException {
        SslContextBuilder builder = SslContextBuilder
                .forClient()
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(applicationProtocolConfig(config, config.isServerHttp2()));
        if (config.isInsecure()) {
            builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        }
        return builder.build();
    }

    public static SslContext ctxForServer(NitmProxyConfig config) throws SSLException {
        return ctxForServer(config, "localhost");
    }

    public static SslContext ctxForServer(NitmProxyConfig config, String serverHost) throws SSLException {
        Certificate certificate = CertUtil.newCert(config.getCertFile(), config.getKeyFile(), serverHost);
        return SslContextBuilder
                .forServer(certificate.getKeyPair().getPrivate(), certificate.getChain())
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(applicationProtocolConfig(config, config.isClientHttp2()))
                .build();
    }

    private static ApplicationProtocolConfig applicationProtocolConfig(NitmProxyConfig config, boolean http2) {
        if (http2) {
            return new ApplicationProtocolConfig(
                    Protocol.ALPN,
                    SelectorFailureBehavior.NO_ADVERTISE,
                    SelectedListenerFailureBehavior.ACCEPT,
                    ApplicationProtocolNames.HTTP_2,
                    ApplicationProtocolNames.HTTP_1_1);
        } else {
            return new ApplicationProtocolConfig(
                    Protocol.ALPN,
                    SelectorFailureBehavior.NO_ADVERTISE,
                    SelectedListenerFailureBehavior.ACCEPT,
                    ApplicationProtocolNames.HTTP_1_1);
        }
    }
}
