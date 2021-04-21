package com.github.chhsiao90.nitmproxy.handler.protocol.tls;

import static io.netty.util.ReferenceCountUtil.safeRelease;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.enums.Handler;
import com.github.chhsiao90.nitmproxy.tls.TlsUtil;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.AbstractSniHandler;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslClientHelloHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;

import javax.net.ssl.SSLException;

public class TlsFrontendHandler extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TlsFrontendHandler.class);

    private NitmProxyMaster master;
    private ConnectionContext connectionContext;

    public TlsFrontendHandler(NitmProxyMaster master, ConnectionContext connectionContext) {
        this.master = master;
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
        ctx.pipeline().replace(this, null, connectionContext.handler(Handler.HTTP1_FRONTEND));
    }

    private void configHttp2(ChannelHandlerContext ctx) {
        ctx.pipeline().replace(this, null, connectionContext.handler(Handler.HTTP2_FRONTEND));
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
                connectionContext.tlsCtx().setEnabled(false);
                connectionContext.tlsCtx().protocolsPromise()
                                 .setSuccess(singletonList(ApplicationProtocolNames.HTTP_1_1));
                configHttp1(tlsCtx);
                ctx.pipeline().remove(SniExtractorHandler.class);
                ctx.pipeline().remove(AlpnNegotiateHandler.class);
                ctx.pipeline().remove(ctx.name());
            } else {
                ctx.pipeline().remove(ctx.name());
            }
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

    private class SniExtractorHandler extends AbstractSniHandler<Object> {

        @Override
        protected Future<Object> lookup(ChannelHandlerContext ctx, String hostname) {
            LOGGER.debug("Client SNI lookup with {}", hostname);
            if (hostname != null) {
                connectionContext.withServerAddr(new Address(hostname, connectionContext.getServerAddr().getPort()));
            }
            return ctx.executor().newSucceededFuture(null);
        }

        @Override
        protected void onLookupComplete(ChannelHandlerContext ctx, String hostname, Future<Object> future) {
            ctx.pipeline().remove(this);
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
