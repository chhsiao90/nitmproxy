package com.github.chhsiao.nitm.nitmproxy.tls;

import com.github.chhsiao.nitm.nitmproxy.NitmProxyConfig;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

public class TlsUtil {
    public static SslContext ctx(NitmProxyConfig config, boolean client, String host)
            throws SSLException, CertificateException {
        return client ? clientCtx(config) : serverCtx(config, host);
    }

    public static SslContext clientCtx(NitmProxyConfig config) throws SSLException {
        return SslContextBuilder
                .forClient()
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                        Protocol.ALPN,
                        SelectorFailureBehavior.NO_ADVERTISE,
                        SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2,
                        ApplicationProtocolNames.HTTP_1_1))
                .build();
    }

    public static SslContext serverCtx(NitmProxyConfig config) throws SSLException {
        return serverCtx(config, "localhost");
    }

    public static SslContext serverCtx(NitmProxyConfig config, String serverHost) throws SSLException {
        Certificate certificate = CertUtil.newCert(config.getCertFile(), config.getKeyFile(), serverHost);
        return SslContextBuilder
                .forServer(certificate.getKeyPair().getPrivate(), certificate.getChain())
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                        Protocol.ALPN,
                        SelectorFailureBehavior.NO_ADVERTISE,
                        SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2,
                        ApplicationProtocolNames.HTTP_1_1))
                .build();
    }
}
