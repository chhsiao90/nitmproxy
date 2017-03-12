package com.github.chhsiao.nitm.nitmproxy.layer.proxy;

import com.github.chhsiao.nitm.nitmproxy.ConnectionInfo;
import com.github.chhsiao.nitm.nitmproxy.NitmProxyConfig;
import com.github.chhsiao.nitm.nitmproxy.layer.protocol.http1.Http1FrontendHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

public class HttpProxyHandler implements ChannelHandler {
    private NitmProxyConfig config;
    private ConnectionInfo connectionInfo;

    public HttpProxyHandler(NitmProxyConfig config, ConnectionInfo connectionInfo) {
        this.config = config;
        this.connectionInfo = connectionInfo;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().replace(this, null, new Http1FrontendHandler(config, connectionInfo));
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) throws Exception {
        ctx.fireExceptionCaught(throwable);
    }
}
