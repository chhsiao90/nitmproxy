package com.github.chhsiao90.nitmproxy.handler;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class TailBackendHandler extends ChannelInboundHandlerAdapter {

    private ConnectionContext connectionContext;

    public TailBackendHandler(ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        connectionContext.clientChannel().writeAndFlush(msg);
    }
}
