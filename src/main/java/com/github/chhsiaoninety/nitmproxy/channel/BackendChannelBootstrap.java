package com.github.chhsiaoninety.nitmproxy.channel;

import com.github.chhsiaoninety.nitmproxy.ConnectionInfo;
import com.github.chhsiaoninety.nitmproxy.NitmProxyMaster;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

public class BackendChannelBootstrap {
    public ChannelFuture connect(ChannelHandlerContext fromCtx, NitmProxyMaster master, ConnectionInfo connectionInfo,
                                 ChannelHandler handler) {
        return new Bootstrap()
                .group(fromCtx.channel().eventLoop())
                .channel(fromCtx.channel().getClass())
                .handler(handler)
                .connect(connectionInfo.getServerAddr().getHost(),
                         connectionInfo.getServerAddr().getPort());
    }
}
