package com.github.chhsiao.nitm.nitmproxy.tls;

import com.github.chhsiao.nitm.nitmproxy.NitmProxyConfig;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

public class TlsUtil {
    public static SslContext ctx(NitmProxyConfig config, boolean client)
            throws SSLException, CertificateException {
        return client ? clientCtx(config) : serverCtx(config);
    }

    public static SslContext clientCtx(NitmProxyConfig config) throws SSLException {
        return SslContextBuilder
                .forClient()
                .sslProvider(sslProvider())
                .build();
    }

    public static SslContext serverCtx(NitmProxyConfig config) throws SSLException, CertificateException {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        return SslContextBuilder
                .forServer(ssc.certificate(), ssc.privateKey())
                .sslProvider(sslProvider())
                .build();
    }

    private static SslProvider sslProvider() {
        return OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
    }
}
