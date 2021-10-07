package com.github.chhsiao90.nitmproxy.handler.protocol.ws;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.handler.protocol.http1.Http1BackendHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.chhsiao90.nitmproxy.http.HttpHeadersUtil.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

public class WebSocketBackendHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketBackendHandler.class);

    private ConnectionContext connectionContext;

    public WebSocketBackendHandler(ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("{} : handlerAdded", connectionContext);
        ctx.pipeline().addAfter(ctx.name(), null, WebSocketClientCompressionHandler.INSTANCE);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("{} : handlerRemoved", connectionContext);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof FullHttpRequest && isWebSocketUpgrade(((FullHttpRequest) msg).headers())) {
            LOGGER.debug("{} : ws upgrading", connectionContext);
        }
        ctx.write(msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpResponse && ((HttpResponse) msg).status() == SWITCHING_PROTOCOLS) {
            configProtocolUpgrade(ctx);
        }
        ctx.fireChannelRead(msg);
    }

    private void configProtocolUpgrade(ChannelHandlerContext ctx) {
        LOGGER.debug("{} : ws upgraded", connectionContext);
        ctx.pipeline().addBefore(ctx.name(), null, new WebSocket13FrameEncoder(true));
        ctx.pipeline().addBefore(ctx.name(), null, new WebSocket13FrameDecoder(
                WebSocketDecoderConfig.newBuilder()
                                      .allowExtensions(true)
                                      .allowMaskMismatch(true)
                                      .build()));

        ChannelHandlerContext httpCtx = ctx.pipeline().context(Http1BackendHandler.class);
        if (httpCtx != null) {
            ctx.pipeline().remove(httpCtx.name());
        }
    }
}
