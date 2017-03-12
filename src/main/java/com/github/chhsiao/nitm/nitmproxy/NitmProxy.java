package com.github.chhsiao.nitm.nitmproxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class NitmProxy {
    public static void main(String[] args) throws Exception {
        NitmProxyConfig config = new NitmProxyConfig();
        config.setMaxContentLength(4096);
        config.setProxyMode(ProxyMode.HTTP);

        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new NitmProxyInitializer(config));
            Channel channel = bootstrap.bind(8080).sync().channel();

            System.err.println("nitm-proxy is listened at http://0.0.0.0:8080");

            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }
}
