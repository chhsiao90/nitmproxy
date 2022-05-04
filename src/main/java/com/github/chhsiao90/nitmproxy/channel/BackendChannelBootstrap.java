package com.github.chhsiao90.nitmproxy.channel;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.Channel;

public class BackendChannelBootstrap {
    public ChannelFuture connect(ChannelHandlerContext fromCtx,
                                 ConnectionContext connectionContext,
                                 ChannelHandler handler) {
        ChannelFuture channelFuture = new Bootstrap()
                .group(fromCtx.channel().eventLoop())
                .channel(fromCtx.channel().getClass())
                .handler(handler)
                .connect(connectionContext.getServerAddr().getHost(),
                         connectionContext.getServerAddr().getPort());
        if (master.config().getOnConnectHandler() != null) {
            channelFuture.addListener((ChannelFutureListener) channelFuture1 -> {
                if (channelFuture1.isSuccess()) {
                    Channel channel = channelFuture1.channel();
                    master.config().getOnConnectHandler().accept(channel);
                }
            });
        }
        return channelFuture;
    }

}
