package com.github.chhsiaoninety.nitmproxy.layer.proxy;

import com.github.chhsiaoninety.nitmproxy.ConnectionInfo;
import com.github.chhsiaoninety.nitmproxy.NitmProxyConfig;
import com.github.chhsiaoninety.nitmproxy.HandlerProvider;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class HttpProxyHandler extends ChannelHandlerAdapter {
    private HandlerProvider handlerProvider;
    private NitmProxyConfig config;
    private ConnectionInfo connectionInfo;

    public HttpProxyHandler(HandlerProvider handlerProvider, NitmProxyConfig config, ConnectionInfo connectionInfo) {
        this.handlerProvider = handlerProvider;
        this.config = config;
        this.connectionInfo = connectionInfo;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().replace(this, null, handlerProvider.http1FrontendHandler(connectionInfo));
    }
}


