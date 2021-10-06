package com.github.chhsiao90.nitmproxy.handler.protocol.ws;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.handler.protocol.http1.Http1BackendHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        ctx.pipeline().addBefore(ctx.name(), null, WebSocketClientCompressionHandler.INSTANCE);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("{} : handlerRemoved", connectionContext);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpResponse && ((HttpResponse) msg).status() == SWITCHING_PROTOCOLS) {
            ctx.pipeline().addBefore(ctx.name(), null, new WebSocket13FrameEncoder(true));
            ctx.pipeline().addBefore(ctx.name(), null, new WebSocket13FrameDecoder(WebSocketDecoderConfig
                    .newBuilder()
                    .allowExtensions(true)
                    .allowMaskMismatch(true)
                    .build()));
            connectionContext.clientChannel().writeAndFlush(msg).addListener(future -> {
                if (future.isSuccess()) {
                    configProtocolUpgrade(ctx);
                    ctx.close();
                }
            });
        } else if (msg instanceof WebSocketFrame) {
            connectionContext.clientChannel().writeAndFlush(msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void configProtocolUpgrade(ChannelHandlerContext ctx) {
        LOGGER.debug("{} : web socket upgrade", connectionContext);
        ctx.pipeline().remove(Http1BackendHandler.class);
    }
}
