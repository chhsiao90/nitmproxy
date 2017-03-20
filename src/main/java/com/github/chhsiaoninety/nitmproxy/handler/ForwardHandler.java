package com.github.chhsiaoninety.nitmproxy.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ForwardHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private Channel outboundChannel;

    public ForwardHandler(Channel outboundChannel) {
        super();
        this.outboundChannel = outboundChannel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        outboundChannel.writeAndFlush(byteBuf);
    }
}
