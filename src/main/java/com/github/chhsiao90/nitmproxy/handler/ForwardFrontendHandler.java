package com.github.chhsiao90.nitmproxy.handler;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ForwardFrontendHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForwardFrontendHandler.class);

    private ConnectionContext connectionContext;

    private List<ChannelHandler> addedHandlers = new ArrayList<>(3);

    public ForwardFrontendHandler(ConnectionContext connectionContext) {
        super();
        this.connectionContext = connectionContext;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("{} : handlerAdded", connectionContext);
        addedHandlers.add(connectionContext.provider().forwardEventHandler());
        addedHandlers.forEach(handler -> ctx.pipeline().addBefore(ctx.name(), null, handler));
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("{} : handlerRemoved", connectionContext);
        addedHandlers.forEach(handler -> ctx.pipeline().remove(handler));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("{} : channelInactive", connectionContext);
        connectionContext.serverChannel().close();
        ctx.fireChannelInactive();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        LOGGER.debug("{} : {}", connectionContext, byteBuf);
        connectionContext.serverChannel().writeAndFlush(byteBuf.retain());
    }
}
