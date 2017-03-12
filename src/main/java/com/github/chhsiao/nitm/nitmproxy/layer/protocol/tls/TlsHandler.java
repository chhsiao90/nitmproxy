package com.github.chhsiao.nitm.nitmproxy.layer.protocol.tls;

import com.github.chhsiao.nitm.nitmproxy.ConnectionInfo;
import com.github.chhsiao.nitm.nitmproxy.NitmProxyConfig;
import com.github.chhsiao.nitm.nitmproxy.tls.TlsUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslHandler;

public class TlsHandler extends ChannelHandlerAdapter {
    private NitmProxyConfig config;
    private ConnectionInfo connectionInfo;
    private boolean client;

    public TlsHandler(NitmProxyConfig config, ConnectionInfo connectionInfo,
                      boolean client) {
        this.config = config;
        this.connectionInfo = connectionInfo;
        this.client = client;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (config.isTls(connectionInfo.getServerAddr().getPort())) {
            SslHandler sslHandler = TlsUtil.ctx(config, client).newHandler(ctx.alloc());
            ctx.pipeline().replace(this, null, sslHandler);
        } else {
            ctx.pipeline().remove(this);
        }
    }
}
