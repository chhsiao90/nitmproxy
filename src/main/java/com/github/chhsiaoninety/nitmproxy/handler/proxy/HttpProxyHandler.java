package com.github.chhsiaoninety.nitmproxy.handler.proxy;

import com.github.chhsiaoninety.nitmproxy.ConnectionInfo;
import com.github.chhsiaoninety.nitmproxy.NitmProxyMaster;
import com.github.chhsiaoninety.nitmproxy.enums.Handler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class HttpProxyHandler extends ChannelHandlerAdapter {
    private NitmProxyMaster master;
    private ConnectionInfo connectionInfo;

    public HttpProxyHandler(NitmProxyMaster master, ConnectionInfo connectionInfo) {
        this.master = master;
        this.connectionInfo = connectionInfo;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().replace(this, null, master.handler(Handler.HTTP1_FRONTEND, connectionInfo, null));
    }
}
