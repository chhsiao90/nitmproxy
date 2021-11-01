package com.github.chhsiao90.nitmproxy.handler;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.chhsiao90.nitmproxy.util.LogWrappers.*;

public class TailFrontendHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TailFrontendHandler.class);

    private ConnectionContext connectionContext;

    public TailFrontendHandler(ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        LOGGER.debug("{} : read {} from server", connectionContext, description(msg));
        ctx.write(msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        LOGGER.debug("{} : write {} to server", connectionContext, description(msg));
        connectionContext.serverChannel().writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        LOGGER.debug("{} : channelInactive", connectionContext);
        connectionContext.close();
    }

}
