package com.github.chhsiao90.nitmproxy.testing;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;

public class EmptyChannelHandler extends ChannelHandlerAdapter {

    private static final EmptyChannelHandler INSTANCE = new EmptyChannelHandler();

    private EmptyChannelHandler() {
    }

    public static ChannelHandler empty() {
        return INSTANCE;
    }
}
