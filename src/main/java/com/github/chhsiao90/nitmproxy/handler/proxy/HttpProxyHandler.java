package com.github.chhsiao90.nitmproxy.handler.proxy;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class HttpProxyHandler extends ChannelHandlerAdapter {
    private ConnectionContext connectionContext;

    public HttpProxyHandler(ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().replace(this, null, connectionContext.provider().http1FrontendHandler());
    }
}
