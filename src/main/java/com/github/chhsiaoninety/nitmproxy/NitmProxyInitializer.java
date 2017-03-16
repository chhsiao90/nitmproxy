package com.github.chhsiaoninety.nitmproxy;

import com.github.chhsiaoninety.nitmproxy.layer.proxy.HttpProxyHandler;
import com.github.chhsiaoninety.nitmproxy.layer.proxy.SocksProxyHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class NitmProxyInitializer extends ChannelInitializer<Channel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NitmProxyInitializer.class);

    private NitmProxyConfig config;

    public NitmProxyInitializer(NitmProxyConfig config) {
        this.config = config;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
        Address clientAddress = new Address(address.getHostName(), address.getPort());
        channel.pipeline().addLast(
                proxyHandler(clientAddress),
                new SimpleChannelInboundHandler<Object>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o)
                            throws Exception {
                        LOGGER.info("[Client ({})] => Unhandled inbound: {}", clientAddress, o);
                    }
                });
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
