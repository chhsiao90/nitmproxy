package com.github.chhsiaoninety.nitmproxy;

import com.github.chhsiaoninety.nitmproxy.channel.BackendChannelBootstrap;
import com.github.chhsiaoninety.nitmproxy.enums.Handler;
import com.github.chhsiaoninety.nitmproxy.handler.proxy.HttpProxyHandler;
import com.github.chhsiaoninety.nitmproxy.handler.proxy.SocksProxyHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

public class NitmProxyMaster {
    private NitmProxyConfig config;
    private HandlerProvider handlerProvider;
    private BackendChannelBootstrap backendChannelBootstrap;

    public NitmProxyMaster(NitmProxyConfig config,
                           HandlerProvider handlerProvider,
                           BackendChannelBootstrap backendChannelBootstrap) {
        this.config = config;
        this.handlerProvider = handlerProvider;
        this.backendChannelBootstrap = backendChannelBootstrap;
    }

    public NitmProxyConfig config() {
        return config;
    }

    public ChannelHandler handler(Handler handler, ConnectionInfo connectionInfo,
                                  Channel outboundChannel) {
        switch (handler) {
        case HTTP1_BACKEND:
            return handlerProvider.http1BackendHandler(this, connectionInfo, outboundChannel);
        case HTTP1_FRONTEND:
            if (outboundChannel == null) {
                return handlerProvider.http1FrontendHandler(this, connectionInfo);
            } else {
                return handlerProvider.http1FrontendHandler(this, connectionInfo, outboundChannel);
            }
        case HTTP2_BACKEND:
            return handlerProvider.http2BackendHandler(this, connectionInfo, outboundChannel);
        case HTTP2_FRONTEND:
            return handlerProvider.http2FrontendHandler(this, connectionInfo, outboundChannel);
        case TLS_BACKEND:
            return handlerProvider.backendTlsHandler(this, connectionInfo, outboundChannel);
        case TLS_FRONTEND:
            return handlerProvider.frontendTlsHandler(this, connectionInfo, outboundChannel);
        default:
            throw new IllegalStateException("No handler found with: " + handler);
        }
    }

    public ChannelHandler proxyHandler(Address clientAddress) {
        switch (config.getProxyMode()) {
        case HTTP:
            return new HttpProxyHandler(this, new ConnectionInfo(clientAddress));
        case SOCKS:
            return new SocksProxyHandler(this, new ConnectionInfo(clientAddress));
        default:
            throw new IllegalStateException("No proxy mode available: " + config.getProxyMode());
        }
    }

    public ChannelFuture connect(ChannelHandlerContext fromCtx, ConnectionInfo connectionInfo,
                                 ChannelHandler handler) {
        return backendChannelBootstrap.connect(fromCtx, this, connectionInfo, handler);
    }
}
