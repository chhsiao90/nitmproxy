package com.github.chhsiaoninety.nitmproxy.handler.protocol.http2;

import com.github.chhsiaoninety.nitmproxy.ConnectionContext;
import com.github.chhsiaoninety.nitmproxy.NitmProxyMaster;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.HttpConversionUtil.ExtensionHeaderNames;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.handler.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http2BackendHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(Http2BackendHandler.class);

    private NitmProxyMaster master;
    private ConnectionContext connectionContext;

    public Http2BackendHandler(NitmProxyMaster master, ConnectionContext connectionContext) {
        this.master = master;
        this.connectionContext = connectionContext;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : channelActive", connectionContext);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : channelInactive", connectionContext);
        connectionContext.clientChannel().close();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : handlerAdded", connectionContext);

        Http2Connection connection = new DefaultHttp2Connection(false);
        ChannelHandler http2ConnHandler = new HttpToHttp2ConnectionHandlerBuilder()
                .frameListener(new DelegatingDecompressorFrameListener(
                        connection,
                        new InboundHttp2ToHttpAdapterBuilder(connection)
                                .maxContentLength(master.config().getMaxContentLength())
                                .propagateSettings(true)
                                .build()))
                .frameLogger(new Http2FrameLogger(LogLevel.DEBUG))
                .connection(connection)
                .build();
        ctx.pipeline()
           .addBefore(ctx.name(), null, http2ConnHandler)
           .addBefore(ctx.name(), null, new Http2Handler());
    }

    private class Http2Handler extends ChannelDuplexHandler {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            LOGGER.info("[Client ({})] <= [Server ({})] : {}",
                        connectionContext.getClientAddr(), connectionContext.getServerAddr(),
                        msg);
            connectionContext.clientChannel().writeAndFlush(msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            LOGGER.info("[Client ({})] => [Server ({})] : {}",
                        connectionContext.getClientAddr(), connectionContext.getServerAddr(),
                        msg);
            if (msg instanceof FullHttpRequest) {
                HttpMessage httpMessage = (HttpRequest) msg;
                httpMessage.headers().add(ExtensionHeaderNames.SCHEME.text(), "https");
            } else if (msg instanceof HttpObject) {
                throw new IllegalStateException("Cannot handle message: " + msg.getClass());
            }

            ctx.writeAndFlush(msg, promise);
        }
    }
}
