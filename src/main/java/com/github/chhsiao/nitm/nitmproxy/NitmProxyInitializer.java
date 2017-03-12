package com.github.chhsiao.nitm.nitmproxy;

import com.github.chhsiao.nitm.nitmproxy.layer.proxy.HttpProxyHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

import java.net.InetSocketAddress;

public class NitmProxyInitializer extends ChannelInitializer<Channel> {
    private NitmProxyConfig config;

    public NitmProxyInitializer(NitmProxyConfig config) {
        this.config = config;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
        Address clientAddress = new Address(address.getHostName(), address.getPort());
        channel.pipeline().addLast(new HttpProxyHandler(config, new ConnectionInfo(clientAddress)));
    }
}
