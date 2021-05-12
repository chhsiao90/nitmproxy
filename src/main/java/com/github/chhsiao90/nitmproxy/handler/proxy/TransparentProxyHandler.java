package com.github.chhsiao90.nitmproxy.handler.proxy;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.handler.protocol.tls.AbstractAlpnHandler;
import com.github.chhsiao90.nitmproxy.tls.TlsUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectDecoder;
import io.netty.handler.ssl.AbstractSniHandler;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslClientHelloHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.util.List;

import static com.github.chhsiao90.nitmproxy.http.HttpHeadersUtil.getContentType;
import static com.google.common.base.Preconditions.checkState;
import static io.netty.util.ReferenceCountUtil.*;
import static java.lang.String.format;

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
                .addBefore(ctx.name(), null, new ConnectToRemote())
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
        ctx.pipeline().replace(this, null, connectionContext.provider().http1FrontendHandler());
    }

    private void configHttp2(ChannelHandlerContext ctx) {
        ctx.pipeline().replace(this, null, connectionContext.provider().http2FrontendHandler());
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
                connectionContext.tlsCtx().protocols(ctx.executor().newPromise());
                connectionContext.tlsCtx().protocol(ctx.executor().newPromise());

                connectionContext.tlsCtx().disableTls();
                configHttp1(tlsCtx);
                ctx.pipeline().remove(SniExtractorHandler.class);
                ctx.pipeline().remove(AlpnNegotiateHandler.class);
                ctx.pipeline().remove(ConnectToRemote.class);
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
                connectionContext.withServerAddr(address);
            }
            return ctx.executor().newSucceededFuture(null);
        }

        @Override
        protected void onLookupComplete(ChannelHandlerContext ctx, String hostname, Future<Object> future) {
            LOGGER.debug("Client SNI lookupComplete with {}", hostname);
            ctx.pipeline().remove(this);
        }
    }

    private class ConnectToRemote extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            connectionContext.connect(connectionContext.getServerAddr(), ctx).addListener((future) -> {
                if (!future.isSuccess()) {
                    ctx.close();
                }
            });
            ctx.fireChannelRead(msg);
        }
    }

    private class AlpnNegotiateHandler extends AbstractAlpnHandler<String> {

        private final ChannelHandlerContext tlsCtx;

        public AlpnNegotiateHandler(ChannelHandlerContext tlsCtx) {
            this.tlsCtx = tlsCtx;
        }

        @Override
        protected Future<String> lookup(ChannelHandlerContext ctx, List<String> protocols) {
            LOGGER.debug("Client ALPN lookup with {}", protocols);
            connectionContext.tlsCtx().protocolsPromise().setSuccess(protocols);
            return ctx.executor().newSucceededFuture(null);
        }

        @Override
        protected void onLookupComplete(ChannelHandlerContext ctx, List<String> protocols,
                                        Future<String> future) throws Exception {
            if (!future.isSuccess()) {
                LOGGER.debug("ALPN negotiate failed with {}", future.cause().getMessage());
                ctx.close();
            } else {
                LOGGER.debug("ALPN negotiated with {}", protocols);
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
