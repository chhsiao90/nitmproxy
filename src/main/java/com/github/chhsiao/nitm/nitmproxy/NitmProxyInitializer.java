package com.github.chhsiao.nitm.nitmproxy;

import com.github.chhsiao.nitm.nitmproxy.layer.proxy.HttpProxyHandler;
import com.github.chhsiao.nitm.nitmproxy.layer.proxy.SocksProxyHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
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
        channel.pipeline().addLast(proxyHandler(clientAddress));
    }

    private ChannelHandler proxyHandler(Address clientAddress) {
        switch (config.getProxyMode()) {
        case HTTP:
            return new HttpProxyHandler(config, new ConnectionInfo(clientAddress));
        case SOCKS:
            return new SocksProxyHandler(config, new ConnectionInfo(clientAddress));
        default:
            throw new IllegalStateException("No proxy mode available: " + config.getProxyMode());
        }
    }
}
