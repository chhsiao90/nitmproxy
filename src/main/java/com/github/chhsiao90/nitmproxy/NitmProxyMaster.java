package com.github.chhsiao90.nitmproxy;

import com.github.chhsiao90.nitmproxy.channel.BackendChannelBootstrap;
import com.github.chhsiao90.nitmproxy.listener.HttpEventListener;
import com.github.chhsiao90.nitmproxy.listener.NitmProxyListenerManager;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

public class NitmProxyMaster {
    private NitmProxyConfig config;
    private BackendChannelBootstrap backendChannelBootstrap;
    private NitmProxyListenerManager nitmProxyListenerManager;

    public NitmProxyMaster(NitmProxyConfig config,
                           BackendChannelBootstrap backendChannelBootstrap) {
        this.config = config;
        this.backendChannelBootstrap = backendChannelBootstrap;
        this.nitmProxyListenerManager = new NitmProxyListenerManager();
    }

    public NitmProxyConfig config() {
        return config;
    }

    public HandlerProvider provider(ConnectionContext context) {
        return new HandlerProvider(this, context);
    }

    public HttpEventListener httpEventListener() {
        return nitmProxyListenerManager;
    }

    public ChannelFuture connect(ChannelHandlerContext fromCtx, ConnectionContext connectionContext,
                                 ChannelHandler handler) {
        return backendChannelBootstrap.connect(fromCtx, this, connectionContext, handler);
    }
}
