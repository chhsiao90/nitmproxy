package com.github.chhsiao90.nitmproxy.handler.protocol.http1;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.Protocols;
import com.github.chhsiao90.nitmproxy.enums.ProxyMode;
import com.github.chhsiao90.nitmproxy.event.OutboundChannelClosedEvent;
import com.github.chhsiao90.nitmproxy.http.HttpUrl;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.github.chhsiao90.nitmproxy.http.HttpUtil.*;
import static com.github.chhsiao90.nitmproxy.util.LogWrappers.*;

public class Http1FrontendHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Http1FrontendHandler.class);

    private NitmProxyMaster master;
    private ConnectionContext connectionContext;
    private boolean tunneled;

    private List<ChannelHandler> addedHandlers = new ArrayList<>(3);

    public Http1FrontendHandler(NitmProxyMaster master, ConnectionContext connectionContext) {
        this.master = master;
        this.connectionContext = connectionContext;
        this.tunneled = connectionContext.connected();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("{} : handlerAdded", connectionContext);

        addedHandlers.add(new HttpServerCodec());
        addedHandlers.add(new HttpObjectAggregator(master.config().getMaxContentLength()));
        addedHandlers.add(connectionContext.provider().http1EventHandler());

        addedHandlers.forEach(handler -> ctx.pipeline().addBefore(ctx.name(), null, handler));
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("{} : handlerRemoved", connectionContext);
        addedHandlers.forEach(handler -> ctx.pipeline().remove(handler));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (connectionContext.connected()) {
            connectionContext.serverChannel().close();
        }
        ctx.fireChannelInactive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof FullHttpRequest)) {
            ctx.fireChannelRead(msg);
            return;
        }
        FullHttpRequest request = (FullHttpRequest) msg;
        if (master.config().getProxyMode() == ProxyMode.HTTP && !tunneled) {
            if (request.method() == HttpMethod.CONNECT) {
                handleTunnelProxyConnection(ctx, request);
            } else {
                handleHttpProxyConnection(ctx, request);
            }
        } else if (master.config().getProxyMode() == ProxyMode.TRANSPARENT && !connectionContext.connected()) {
            handleTransparentProxyConnection(ctx, request);
        } else {
            LOGGER.debug("{} : {}", connectionContext, className(request));
            connectionContext.serverChannel().writeAndFlush(request);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof OutboundChannelClosedEvent) {
            if (tunneled) {
                ctx.close();
            }
        }

        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof HttpResponse)) {
            ctx.write(msg, promise);
            return;
        }
        HttpResponse response = (HttpResponse) msg;
        if (response.status() == HttpResponseStatus.SWITCHING_PROTOCOLS) {
            ctx.writeAndFlush(msg, promise);
            ctx.pipeline().replace(ctx.name(), null, connectionContext.provider().forwardFrontendHandler());
        } else {
            ctx.write(msg, promise);
        }
    }

    private void handleTunnelProxyConnection(ChannelHandlerContext ctx,
                                             FullHttpRequest request) throws Exception {
        try {
            Address address = Address.resolve(request.uri(), HTTPS_PORT);
            connectionContext.connect(address, ctx).addListener((future) -> {
                if (!future.isSuccess()) {
                    ctx.close();
                }
            });
            FullHttpResponse response =
                    new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
            LOGGER.debug("{} : {}", connectionContext, className(response));
            ctx.writeAndFlush(response);
            ctx.pipeline().replace(Http1FrontendHandler.this, null,
                    connectionContext.provider().tlsFrontendHandler());
        } finally {
            request.release();
        }
    }

    private void handleHttpProxyConnection(ChannelHandlerContext ctx,
                                           FullHttpRequest request) throws Exception {
        HttpUrl httpUrl = HttpUrl.resolve(request.uri());
        Address address = new Address(httpUrl.getHost(), httpUrl.getPort());
        request.setUri(httpUrl.getPath());
        connectionContext.connect(address, ctx).addListener((ChannelFuture future) -> {
            if (future.isSuccess()) {
                LOGGER.debug("{} : {}", connectionContext, className(request));
                future.channel().writeAndFlush(request);
            } else {
                request.release();
                ctx.channel().close();
            }
        });
        if (!connectionContext.tlsCtx().isNegotiated()) {
            connectionContext.tlsCtx().disableTls();
            connectionContext.tlsCtx().protocolPromise().setSuccess(Protocols.HTTP_1);
        }
    }

    private void handleTransparentProxyConnection(ChannelHandlerContext ctx, FullHttpRequest request) {
        Address address = Address.resolve(request.headers().get(HttpHeaderNames.HOST), HTTP_PORT);
        connectionContext.connect(address, ctx).addListener((ChannelFuture future) -> {
            if (future.isSuccess()) {
                LOGGER.debug("{} : {}", connectionContext, className(request));
                future.channel().writeAndFlush(request);
            } else {
                request.release();
                ctx.channel().close();
            }
        });
        connectionContext.tlsCtx().disableTls();
        connectionContext.tlsCtx().protocolPromise().setSuccess(Protocols.HTTP_1);
    }
}
