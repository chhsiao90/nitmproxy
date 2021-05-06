package com.github.chhsiao90.nitmproxy.handler.proxy;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.handler.protocol.tls.AbstractAlpnHandler;
import com.github.chhsiao90.nitmproxy.handler.protocol.tls.TlsFrontendHandler;
import com.github.chhsiao90.nitmproxy.listener.HttpListener;
import com.github.chhsiao90.nitmproxy.tls.TlsUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandResponse;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.codec.socksx.v4.Socks4CommandStatus;
import io.netty.handler.codec.socksx.v4.Socks4CommandType;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.ssl.*;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.chhsiao90.nitmproxy.http.HttpHeadersUtil.getContentType;
import static com.google.common.base.Preconditions.checkState;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty.util.ReferenceCountUtil.*;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;

public class TransparentProxyHandler extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransparentProxyHandler.class);

    private ConnectionContext connectionContext;

    public TransparentProxyHandler(
            NitmProxyMaster master,
            ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        LOGGER.debug("{} : handlerAdded", connectionContext);
        ctx.pipeline()
                .addBefore(ctx.name(), null, new DetectSslHandler(ctx))
                .addBefore(ctx.name(), null, new SniExtractorHandler())
                .addBefore(ctx.name(), null, new AlpnNegotiateHandler(ctx));
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        LOGGER.debug("{} : handlerRemoved", connectionContext);

        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.debug("{} : channelInactive", connectionContext);
        if (connectionContext.connected()) {
            connectionContext.serverChannel().close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error(format("%s : exceptionCaught with %s",
                connectionContext, cause.getMessage()),
                cause);
        ctx.close();
    }

    private SslHandler sslHandler(ByteBufAllocator alloc) throws SSLException {
        return TlsUtil.ctxForServer(connectionContext).newHandler(alloc);
    }

    private void configHttp1(ChannelHandlerContext ctx) {
        ctx.pipeline().replace(this, null, connectionContext.provider().http1EventHandler());
    }

    private void configHttp2(ChannelHandlerContext ctx) {
        ctx.pipeline().replace(this, null, connectionContext.provider().http2EventHandler());
    }

    private class DetectSslHandler extends SslClientHelloHandler<Boolean> {

        private final ChannelHandlerContext tlsCtx;

        private DetectSslHandler(ChannelHandlerContext tlsCtx) {
            this.tlsCtx = tlsCtx;
        }

        @Override
        protected Future<Boolean> lookup(ChannelHandlerContext ctx, ByteBuf byteBuf) {
            boolean ssl = byteBuf != null;
            LOGGER.debug("SSL detection with {}", ssl);
            return ctx.executor().newSucceededFuture(ssl);
        }

        @Override
        protected void onLookupComplete(ChannelHandlerContext ctx, Future<Boolean> future) {
            if (!future.isSuccess()) {
                LOGGER.debug("SSL detection failed with {}", future.cause().getMessage());
                ctx.close();
            } else if (!future.getNow()) {
                connectionContext.tlsCtx().disableTls();
                configHttp1(tlsCtx);
                ctx.pipeline().remove(SniExtractorHandler.class);
                ctx.pipeline().remove(AlpnNegotiateHandler.class);
                ctx.pipeline().remove(ctx.name());
            } else {
                ctx.pipeline().remove(ctx.name());
            }
        }
    }
    private class SniExtractorHandler extends AbstractSniHandler<Object> {

        @Override
        protected Future<Object> lookup(ChannelHandlerContext ctx, String hostname) {
            LOGGER.debug("Client SNI lookup with {}", hostname);
            if (hostname != null) {
                Address address = new Address(hostname, 443);
                connectionContext.connect(address, ctx).addListener((future) -> {
                    if (!future.isSuccess()) {
                        ctx.close();
                    }
                });
                connectionContext.withServerAddr(address);
            }
            return ctx.executor().newSucceededFuture(null);
        }

        @Override
        protected void onLookupComplete(ChannelHandlerContext ctx, String hostname, Future<Object> future) {
            ctx.pipeline().remove(this);
        }
    }

    private class AlpnNegotiateHandler extends AbstractAlpnHandler<String> {

        private final ChannelHandlerContext tlsCtx;

        public AlpnNegotiateHandler(ChannelHandlerContext tlsCtx) {
            this.tlsCtx = tlsCtx;
        }

        @Override
        protected void onLookupComplete(ChannelHandlerContext ctx, List<String> protocols,
                                        Future<String> future) throws Exception {
            if (!future.isSuccess()) {
                LOGGER.debug("ALPN negotiate failed with {}", future.cause().getMessage());
                ctx.close();
            } else {
                LOGGER.debug("ALPN negotiated with {}", future.getNow());
                SslHandler sslHandler = sslHandler(ctx.alloc());
                try {
                    ctx.pipeline()
                            .addAfter(ctx.name(), null, new AlpnHandler(tlsCtx))
                            .replace(ctx.name(), null, sslHandler);
                    sslHandler = null;
                } finally {
                    if (sslHandler != null) {
                        safeRelease(sslHandler.engine());
                    }
                }
            }
        }

        @Override
        protected Future<String> lookup(ChannelHandlerContext ctx, List<String> protocols) {
            LOGGER.debug("Client ALPN lookup with {}", protocols);
            connectionContext.tlsCtx().protocolsPromise().setSuccess(protocols);
            return connectionContext.tlsCtx().protocolPromise();
        }
    }

    private class AlpnHandler extends ApplicationProtocolNegotiationHandler {

        private ChannelHandlerContext tlsCtx;

        private AlpnHandler(ChannelHandlerContext tlsCtx) {
            super(ApplicationProtocolNames.HTTP_1_1);
            this.tlsCtx = tlsCtx;
        }

        @Override
        protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
            if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
                configHttp1(tlsCtx);
            } else if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
                configHttp2(tlsCtx);
            } else {
                throw new IllegalStateException("unknown protocol: " + protocol);
            }
        }
    }
}
