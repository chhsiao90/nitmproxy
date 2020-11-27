package com.github.chhsiaoninety.nitmproxy.channel;

import com.github.chhsiaoninety.nitmproxy.ConnectionContext;
import com.github.chhsiaoninety.nitmproxy.NitmProxyMaster;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

public class BackendChannelBootstrap {
    public ChannelFuture connect(ChannelHandlerContext fromCtx, NitmProxyMaster master, ConnectionContext connectionContext,
                                 ChannelHandler handler) {
        return new Bootstrap()
                .group(fromCtx.channel().eventLoop())
                .channel(fromCtx.channel().getClass())
                .handler(handler)
                .connect(connectionContext.getServerAddr().getHost(),
                         connectionContext.getServerAddr().getPort());
    }
}
