package com.github.chhsiao90.nitmproxy.tls;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.listener.HttpListener;
import com.google.common.io.Resources;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.io.Resources.*;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
import static java.lang.String.*;
import static java.nio.charset.StandardCharsets.*;

public class SimpleUnsafeAccessSupport implements UnsafeAccessSupport {

    private static final String ACCEPT_MAGIC = ";nitmproxy-unsafe=accept";
    private static final String DENY_MAGIC = ";nitmproxy-unsafe=deny";

    private final ConcurrentMap<Address, UnsafeAccess> accepted;
    private final String askTemplate;
    private final Interceptor interceptor;

    @SuppressWarnings("UnstableApiUsage")
    public SimpleUnsafeAccessSupport() throws IOException {
        this(Resources.toString(getResource("html-templates/ask-unsafe-access.html"), UTF_8));
    }

    public SimpleUnsafeAccessSupport(String askTemplate) {
        this.accepted = new ConcurrentHashMap<>();
        this.askTemplate = askTemplate;
        this.interceptor = new Interceptor();
    }

    @Override
    public UnsafeAccess checkUnsafeAccess(ConnectionContext context, X509Certificate[] chain,
            CertificateException cause) {
        accepted.putIfAbsent(context.getServerAddr(), UnsafeAccess.ASK);
        return accepted.get(context.getServerAddr());
    }

    @Override
    public TrustManagerFactory create(TrustManagerFactory delegate, ConnectionContext context) {
        return UnsafeAccessSupportTrustManagerFactory.create(delegate, this, context);
    }

    public HttpListener getInterceptor() {
        return interceptor;
    }

    class Interceptor implements HttpListener {
        @Override
        public Optional<FullHttpResponse> onHttp1Request(ConnectionContext connectionContext, FullHttpRequest request) {
            if (connectionContext.getServerAddr() == null || !accepted.containsKey(connectionContext.getServerAddr())) {
                return Optional.empty();
            }
            switch (accepted.get(connectionContext.getServerAddr())) {
                case ASK:
                    return handleAsk(connectionContext, request);
                case DENY:
                    return Optional.of(createDenyResponse());
                case ACCEPT:
                default:
                    return Optional.empty();
            }
        }

        private Optional<FullHttpResponse> handleAsk(ConnectionContext connectionContext, FullHttpRequest request) {
            if (request.uri().endsWith(ACCEPT_MAGIC)) {
                request.setUri(request.uri().replace(ACCEPT_MAGIC, ""));
                accepted.put(connectionContext.getServerAddr(), UnsafeAccess.ACCEPT);
                return Optional.empty();
            }
            if (request.uri().endsWith(DENY_MAGIC)) {
                accepted.put(connectionContext.getServerAddr(), UnsafeAccess.DENY);
                return Optional.of(createDenyResponse());
            }
            return Optional.of(createAskResponse(request.uri()));
        }

        private FullHttpResponse createDenyResponse() {
            return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);
        }

        private FullHttpResponse createAskResponse(String uri) {
            DefaultFullHttpResponse response =  new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.content().writeCharSequence(format(askTemplate, uri + ACCEPT_MAGIC, uri + DENY_MAGIC), UTF_8);
            response.headers().set(CONTENT_TYPE, TEXT_HTML);
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            return response;
        }
    }
}
