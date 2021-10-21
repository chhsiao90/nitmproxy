package com.github.chhsiao90.nitmproxy.channel;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

public class BackendChannelBootstrap {
    public ChannelFuture connect(ChannelHandlerContext fromCtx,
                                 ConnectionContext connectionContext,
                                 ChannelHandler handler) {
        return new Bootstrap()
                .group(fromCtx.channel().eventLoop())
                .channel(fromCtx.channel().getClass())
                .handler(handler)
                .connect(connectionContext.getServerAddr().getHost(),
                         connectionContext.getServerAddr().getPort());
    }
}
