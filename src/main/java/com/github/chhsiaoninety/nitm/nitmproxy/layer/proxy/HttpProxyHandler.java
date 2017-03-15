package com.github.chhsiaoninety.nitm.nitmproxy.layer.proxy;

import com.github.chhsiaoninety.nitm.nitmproxy.ConnectionInfo;
import com.github.chhsiaoninety.nitm.nitmproxy.NitmProxyConfig;
import com.github.chhsiaoninety.nitm.nitmproxy.layer.protocol.http1.Http1FrontendHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class HttpProxyHandler extends ChannelHandlerAdapter {
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
}


