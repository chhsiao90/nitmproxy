package com.github.chhsiaoninety.nitmproxy;

import com.github.chhsiaoninety.nitmproxy.channel.BackendChannelBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class NitmProxyInitializer extends ChannelInitializer<Channel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NitmProxyInitializer.class);

    private NitmProxyMaster master;

    public NitmProxyInitializer(NitmProxyConfig config) {
        this(new NitmProxyMaster(config, new HandlerProvider(), new BackendChannelBootstrap()));
    }

    public NitmProxyInitializer(NitmProxyMaster master) {
        this.master = master;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
        Address clientAddress = new Address(address.getHostName(), address.getPort());
        channel.pipeline().addLast(
                master.proxyHandler(clientAddress),
                new SimpleChannelInboundHandler<Object>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o)
                            throws Exception {
                        LOGGER.info("[Client ({})] => Unhandled inbound: {}", clientAddress, o);
                    }
                });
    }
}
