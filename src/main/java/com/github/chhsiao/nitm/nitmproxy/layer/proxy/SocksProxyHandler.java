package com.github.chhsiao.nitm.nitmproxy.layer.proxy;

import com.github.chhsiao.nitm.nitmproxy.Address;
import com.github.chhsiao.nitm.nitmproxy.ConnectionInfo;
import com.github.chhsiao.nitm.nitmproxy.NitmProxyConfig;
import com.github.chhsiao.nitm.nitmproxy.layer.protocol.http1.Http1BackendHandler;
import com.github.chhsiao.nitm.nitmproxy.layer.protocol.http1.Http1FrontendHandler;
import com.github.chhsiao.nitm.nitmproxy.layer.protocol.tls.TlsHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandResponse;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.codec.socksx.v4.Socks4CommandStatus;
import io.netty.handler.codec.socksx.v4.Socks4CommandType;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressEncoder;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SocksProxyHandler extends SimpleChannelInboundHandler<SocksMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SocksProxyHandler.class);

    private NitmProxyConfig config;
    private ConnectionInfo connectionInfo;

    private List<ChannelHandler> handlers;

    public SocksProxyHandler(NitmProxyConfig config, ConnectionInfo connectionInfo) {
        this.config = config;
        this.connectionInfo = connectionInfo;

        handlers = new ArrayList<>();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : SocksProxyHandler handlerAdded", connectionInfo);

        Socks5ServerEncoder socks5ServerEncoder = addChannelHandler(
                new Socks5ServerEncoder(Socks5AddressEncoder.DEFAULT));
        SocksPortUnificationServerHandler socksPortUnificationServerHandler = new SocksPortUnificationServerHandler(socks5ServerEncoder);
        ctx.pipeline().addBefore(ctx.name(), null, socksPortUnificationServerHandler);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : SocksProxyHandler handlerRemoved", connectionInfo);

        handlers.forEach(ctx.pipeline()::remove);
        handlers.clear();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage socksMessage)
            throws Exception {
        switch (socksMessage.version()) {
        case SOCKS4a:
            Socks4CommandRequest socksV4CmdRequest = (Socks4CommandRequest) socksMessage;
            if (socksV4CmdRequest.type() == Socks4CommandType.CONNECT) {
                onSocksSuccess(ctx, socksV4CmdRequest);
            } else {
                ctx.close();
            }
            break;
        case SOCKS5:
            if (socksMessage instanceof Socks5InitialRequest) {
                ctx.pipeline().addFirst(addChannelHandler(new Socks5CommandRequestDecoder()));
                ctx.writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
            } else if (socksMessage instanceof Socks5PasswordAuthRequest) {
                ctx.pipeline().addFirst(addChannelHandler(new Socks5CommandRequestDecoder()));
                ctx.writeAndFlush(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
            } else if (socksMessage instanceof Socks5CommandRequest) {
                Socks5CommandRequest socks5CmdRequest = (Socks5CommandRequest) socksMessage;
                if (socks5CmdRequest.type() == Socks5CommandType.CONNECT) {
                    onSocksSuccess(ctx, socks5CmdRequest);
                } else {
                    ctx.close();
                }
            } else {
                ctx.close();
            }
            break;
        case UNKNOWN:
            ctx.close();
            break;
        }
    }

    private <T extends ChannelHandler> T addChannelHandler(T channelHandler) {
        handlers.add(channelHandler);
        return channelHandler;
    }

    private void onSocksSuccess(ChannelHandlerContext ctx, Socks4CommandRequest request) {
        Address serverAddr = new Address(request.dstAddr(), request.dstPort());
        createServerChannel(ctx, serverAddr).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    ConnectionInfo newConnectionInfo = new ConnectionInfo(
                            connectionInfo.getClientAddr(), serverAddr);
                    ctx.writeAndFlush(new DefaultSocks4CommandResponse(
                            Socks4CommandStatus.SUCCESS,
                            request.dstAddr(),
                            request.dstPort()));

                    onServerConnected(ctx, newConnectionInfo, future.channel());
                } else {
                    ctx.channel().writeAndFlush(new DefaultSocks4CommandResponse(
                            Socks4CommandStatus.REJECTED_OR_FAILED,
                            request.dstAddr(),
                            request.dstPort()));
                    ctx.close();
                }
            }
        });
    }

    private void onSocksSuccess(ChannelHandlerContext ctx, Socks5CommandRequest request) {
        Address serverAddr = new Address(request.dstAddr(), request.dstPort());
        createServerChannel(ctx, serverAddr).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    ConnectionInfo newConnectionInfo = new ConnectionInfo(
                            connectionInfo.getClientAddr(), serverAddr);
                    ctx.writeAndFlush(new DefaultSocks5CommandResponse(
                            Socks5CommandStatus.SUCCESS,
                            request.dstAddrType(),
                            request.dstAddr(),
                            request.dstPort()));

                    onServerConnected(ctx, newConnectionInfo, future.channel());
                } else {
                    ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                            Socks5CommandStatus.FAILURE,
                            request.dstAddrType(),
                            request.dstAddr(),
                            request.dstPort()));
                    ctx.close();
                }
            }
        });
    }

    private void onServerConnected(ChannelHandlerContext ctx, ConnectionInfo connectionInfo, Channel outboundChannel) {
        Http1FrontendHandler handler = new Http1FrontendHandler(
                config, connectionInfo, outboundChannel);
        ctx.pipeline()
           .addBefore(ctx.name(), null, new TlsHandler(config, connectionInfo, false))
           .replace(SocksProxyHandler.this, null, handler);
    }

    private ChannelFuture createServerChannel(ChannelHandlerContext ctx, Address serverAddr) {
        ConnectionInfo newConnectionInfo = new ConnectionInfo(
                connectionInfo.getClientAddr(), new Address(serverAddr.getHost(), serverAddr.getPort()));
        Bootstrap bootstrap = new Bootstrap()
                .group(ctx.channel().eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast(
                                new TlsHandler(config, newConnectionInfo, true),
                                new Http1BackendHandler(config, newConnectionInfo, ctx.channel()));
                    }
                });
        return bootstrap.connect(serverAddr.getHost(), serverAddr.getPort());
    }
}