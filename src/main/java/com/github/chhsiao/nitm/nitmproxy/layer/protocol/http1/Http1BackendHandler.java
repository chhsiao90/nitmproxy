package com.github.chhsiao.nitm.nitmproxy.layer.protocol.http1;

import com.github.chhsiao.nitm.nitmproxy.ConnectionInfo;
import com.github.chhsiao.nitm.nitmproxy.NitmProxyConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObject;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http1BackendHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Http1BackendHandler.class);

    private NitmProxyConfig config;
    private ConnectionInfo connectionInfo;
    private Channel outboundChannel;

    public Http1BackendHandler(NitmProxyConfig config, ConnectionInfo connectionInfo,
                               Channel outboundChannel) {
        this.config = config;
        this.connectionInfo = connectionInfo;
        this.outboundChannel = outboundChannel;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : Http1BackendHandler handlerAdded", connectionInfo);

        ctx.pipeline().addBefore(ctx.name(), null, new HttpClientCodec());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, HttpObject httpObject)
            throws Exception {
        LOGGER.info("[Client ({})] <= [Server ({})] : {}",
                    connectionInfo.getClientAddr(), connectionInfo.getServerAddr(),
                    httpObject);

        outboundChannel.writeAndFlush(ReferenceCountUtil.retain(httpObject));
    }
}
