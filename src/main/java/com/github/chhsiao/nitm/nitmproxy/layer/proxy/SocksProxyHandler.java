package com.github.chhsiao.nitm.nitmproxy.layer.proxy;

import com.github.chhsiao.nitm.nitmproxy.Address;
import com.github.chhsiao.nitm.nitmproxy.ConnectionInfo;
import com.github.chhsiao.nitm.nitmproxy.NitmProxyConfig;
import com.github.chhsiao.nitm.nitmproxy.layer.protocol.http1.Http1BackendHandler;
import com.github.chhsiao.nitm.nitmproxy.layer.protocol.http1.Http1FrontendHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
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
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocksProxyHandler extends SimpleChannelInboundHandler<SocksMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SocksProxyHandler.class);

    private NitmProxyConfig config;
    private ConnectionInfo connectionInfo;

    public SocksProxyHandler(NitmProxyConfig config, ConnectionInfo connectionInfo) {
        this.config = config;
        this.connectionInfo = connectionInfo;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : SocksProxyHandler handlerAdded", connectionInfo);

        ctx.pipeline().addBefore(ctx.name(), null, new SocksPortUnificationServerHandler());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : SocksProxyHandler handlerRemoved", connectionInfo);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage socksMessage)
            throws Exception {
        switch (socksMessage.version()) {
        case SOCKS4a:
            Socks4CommandRequest socksV4CmdRequest = (Socks4CommandRequest) socksMessage;
            if (socksV4CmdRequest.type() == Socks4CommandType.CONNECT) {
                onSuccess(ctx, socksV4CmdRequest);
            } else {
                ctx.close();
            }
            break;
        case SOCKS5:
            if (socksMessage instanceof Socks5InitialRequest) {
                ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                ctx.writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
            } else if (socksMessage instanceof Socks5PasswordAuthRequest) {
                ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                ctx.writeAndFlush(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
            } else if (socksMessage instanceof Socks5CommandRequest) {
                Socks5CommandRequest socks5CmdRequest = (Socks5CommandRequest) socksMessage;
                if (socks5CmdRequest.type() == Socks5CommandType.CONNECT) {
                    onSuccess(ctx, socks5CmdRequest);
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

    private void onSuccess(ChannelHandlerContext ctx, Socks4CommandRequest request) {
        Address serverAddr = new Address(request.dstAddr(), request.dstPort());
        createServerChannel(ctx, serverAddr).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    ctx.channel().writeAndFlush(new DefaultSocks4CommandResponse(
                            Socks4CommandStatus.SUCCESS,
                            request.dstAddr(),
                            request.dstPort()));
                    Http1FrontendHandler handler = new Http1FrontendHandler(
                            config, new ConnectionInfo(connectionInfo.getClientAddr(), serverAddr), future.channel());
                    ctx.pipeline().replace(SocksProxyHandler.this, null, handler);
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

    private void onSuccess(ChannelHandlerContext ctx, Socks5CommandRequest request) {
        Address serverAddr = new Address(request.dstAddr(), request.dstPort());
        createServerChannel(ctx, serverAddr).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                            Socks5CommandStatus.SUCCESS,
                            request.dstAddrType(),
                            request.dstAddr(),
                            request.dstPort()));
                    Http1FrontendHandler handler = new Http1FrontendHandler(
                            config, new ConnectionInfo(connectionInfo.getClientAddr(), serverAddr), future.channel());
                    ctx.pipeline().replace(SocksProxyHandler.this, null, handler);
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

    private ChannelFuture createServerChannel(ChannelHandlerContext ctx, Address serverAddr) {
        ConnectionInfo newConnectionInfo = new ConnectionInfo(
                connectionInfo.getClientAddr(), new Address(serverAddr.getHost(), serverAddr.getPort()));
        Bootstrap bootstrap = new Bootstrap()
                .group(ctx.channel().eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new Http1BackendHandler(config, newConnectionInfo, ctx.channel()));
        return bootstrap.connect(serverAddr.getHost(), serverAddr.getPort());
    }
}
