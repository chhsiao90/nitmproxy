package com.github.chhsiao90.nitmproxy.handler.protocol.http1;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http1BackendHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Http1BackendHandler.class);

    private ConnectionContext connectionContext;

    public Http1BackendHandler(ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("{} : handlerAdded", connectionContext);
        ctx.pipeline().addBefore(ctx.name(), null, new HttpClientCodec());
        ctx.pipeline().addBefore(ctx.name(), null, connectionContext.provider().wsBackendHandler());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        LOGGER.debug("{} : handlerRemoved", connectionContext);
        ctx.pipeline().remove(HttpClientCodec.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof HttpObject)) {
            ctx.fireChannelRead(msg);
            return;
        }
        if (msg instanceof HttpResponse && ((HttpResponse) msg).status() == HttpResponseStatus.SWITCHING_PROTOCOLS) {
            ctx.pipeline().replace(ctx.name(), null, connectionContext.provider().forwardBackendHandler());
        }
        connectionContext.clientChannel().writeAndFlush(msg);
    }
}
