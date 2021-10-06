package com.github.chhsiao90.nitmproxy.handler.protocol.ws;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.handler.protocol.http1.Http1FrontendHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.chhsiao90.nitmproxy.util.LogWrappers.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

public class WebSocketFrontendHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketFrontendHandler.class);

    private ConnectionContext connectionContext;

    public WebSocketFrontendHandler(ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("{} : handlerAdded", connectionContext);
        ctx.pipeline().addBefore(ctx.name(), null, new WebSocketServerCompressionHandler());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("{} : handlerRemoved", connectionContext);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof WebSocketFrame) {
            LOGGER.debug("{} : read {}", connectionContext, className(msg));
            connectionContext.serverChannel().writeAndFlush(msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof HttpResponse && ((HttpResponse) msg).status() == SWITCHING_PROTOCOLS) {
            ctx.pipeline().addBefore(ctx.name(), null, new WebSocket13FrameEncoder(false));
            ctx.pipeline().addBefore(ctx.name(), null, new WebSocket13FrameDecoder(
                    WebSocketDecoderConfig.newBuilder()
                                          .allowExtensions(true)
                                          .allowMaskMismatch(true)
                                          .build()));
            ctx.write(msg).addListener(future -> {
                if (future.isSuccess()) {
                    configProtocolUpgrade(ctx);
                    promise.setSuccess();
                } else {
                    promise.setFailure(future.cause());
                }
            });
        } else if (msg instanceof WebSocketFrame) {
            LOGGER.debug("{} : write {}", connectionContext, className(msg));
            ctx.write(msg, promise);
        } else {
            ctx.write(msg, promise);
        }
    }

    private void configProtocolUpgrade(ChannelHandlerContext ctx) {
        LOGGER.debug("{} : web socket upgraded", connectionContext);
        ctx.pipeline().remove(Http1FrontendHandler.class);
    }
}
