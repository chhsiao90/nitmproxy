package com.github.chhsiao90.nitmproxy.handler.proxy;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.enums.Handler;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class HttpProxyHandler extends ChannelHandlerAdapter {
    private NitmProxyMaster master;
    private ConnectionContext connectionContext;

    public HttpProxyHandler(NitmProxyMaster master, ConnectionContext connectionContext) {
        this.master = master;
        this.connectionContext = connectionContext;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().replace(this, null, connectionContext.handler(Handler.HTTP1_FRONTEND));
    }
}
