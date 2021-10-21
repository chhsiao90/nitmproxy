package com.github.chhsiao90.nitmproxy;

import com.github.chhsiao90.nitmproxy.channel.BackendChannelBootstrap;
import com.github.chhsiao90.nitmproxy.listener.NitmProxyListenerManagerProvider;
import com.github.chhsiao90.nitmproxy.listener.NitmProxyListenerProvider;
import com.github.chhsiao90.nitmproxy.tls.CertManager;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

public class NitmProxyMaster {

    private NitmProxyConfig config;
    private BackendChannelBootstrap backendChannelBootstrap;
    private NitmProxyListenerManagerProvider listenerProvider;
    private CertManager certManager;

    public NitmProxyMaster(NitmProxyConfig config,
                           BackendChannelBootstrap backendChannelBootstrap) {
        this.config = config;
        this.backendChannelBootstrap = backendChannelBootstrap;
        this.listenerProvider = new NitmProxyListenerManagerProvider(config.getListeners());
        this.certManager = new CertManager(config);
    }

    public NitmProxyConfig config() {
        return config;
    }

    public HandlerProvider provider(ConnectionContext context) {
        return new HandlerProvider(this, context);
    }

    public NitmProxyListenerProvider listenerProvider() {
        return listenerProvider;
    }

    public CertManager certManager() {
        return certManager;
    }

    public ChannelFuture connect(ChannelHandlerContext fromCtx, ConnectionContext connectionContext,
                                 ChannelHandler handler) {
        return backendChannelBootstrap.connect(fromCtx, this, connectionContext, handler);
    }
}
