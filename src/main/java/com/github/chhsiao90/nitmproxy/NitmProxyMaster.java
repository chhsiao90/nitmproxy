package com.github.chhsiao90.nitmproxy;

import com.github.chhsiao90.nitmproxy.channel.BackendChannelBootstrap;
import com.github.chhsiao90.nitmproxy.listener.ForwardListener;
import com.github.chhsiao90.nitmproxy.listener.HttpListener;
import com.github.chhsiao90.nitmproxy.listener.NitmProxyListenerManager;
import com.github.chhsiao90.nitmproxy.tls.CertManager;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

public class NitmProxyMaster {

    private NitmProxyConfig config;
    private BackendChannelBootstrap backendChannelBootstrap;
    private NitmProxyListenerManager nitmProxyListenerManager;
    private CertManager certManager;

    public NitmProxyMaster(NitmProxyConfig config,
                           BackendChannelBootstrap backendChannelBootstrap) {
        this.config = config;
        this.backendChannelBootstrap = backendChannelBootstrap;
        this.nitmProxyListenerManager = new NitmProxyListenerManager(
                config.getHttpListeners(), config.getForwardListeners());
        this.certManager = new CertManager(config);
    }

    public NitmProxyConfig config() {
        return config;
    }

    public HandlerProvider provider(ConnectionContext context) {
        return new HandlerProvider(this, context);
    }

    public HttpListener httpEventListener() {
        return nitmProxyListenerManager;
    }

    public ForwardListener forwardEventListener() {
        return nitmProxyListenerManager;
    }

    public CertManager certManager() {
        return certManager;
    }

    public ChannelFuture connect(ChannelHandlerContext fromCtx, ConnectionContext connectionContext,
                                 ChannelHandler handler) {
        return backendChannelBootstrap.connect(fromCtx, this, connectionContext, handler);
    }
}
