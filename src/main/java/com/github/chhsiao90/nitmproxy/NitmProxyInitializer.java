package com.github.chhsiao90.nitmproxy;

import com.github.chhsiao90.nitmproxy.channel.BackendChannelBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class NitmProxyInitializer extends ChannelInitializer<Channel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NitmProxyInitializer.class);

    private NitmProxyMaster master;

    public NitmProxyInitializer(NitmProxyConfig config) {
        this(new NitmProxyMaster(config, new BackendChannelBootstrap()));
    }

    public NitmProxyInitializer(NitmProxyMaster master) {
        this.master = master;
    }

    @Override
    protected void initChannel(Channel channel) {
        InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
        Address clientAddress = new Address(address.getHostName(), address.getPort());

        ConnectionContext context = new ConnectionContext(master)
                .withClientAddr(clientAddress)
                .withClientChannel(channel);
        context.listener().onInit(context, channel);

        LOGGER.debug("{} : connection init", context);

        channel.pipeline().replace(this, null, context.proxyHandler());
        channel.pipeline().addLast(context.provider().tailFrontendHandler());
    }
}
