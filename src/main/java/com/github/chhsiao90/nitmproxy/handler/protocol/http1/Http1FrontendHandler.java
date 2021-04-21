package com.github.chhsiao90.nitmproxy.handler.protocol.http1;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.enums.ProxyMode;
import com.github.chhsiao90.nitmproxy.event.OutboundChannelClosedEvent;
import com.github.chhsiao90.nitmproxy.http.HttpUrl;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Http1FrontendHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Http1FrontendHandler.class);

    private NitmProxyMaster master;
    private ConnectionContext connectionContext;
    private boolean tunneled;

    private List<ChannelHandler> addedHandlers = new ArrayList<>(3);

    public Http1FrontendHandler(NitmProxyMaster master, ConnectionContext connectionContext) {
        super();
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
    protected void channelRead0(ChannelHandlerContext ctx,
                                FullHttpRequest request) throws Exception {
        if (master.config().getProxyMode() == ProxyMode.HTTP && !tunneled) {
            if (request.method() == HttpMethod.CONNECT) {
                handleTunnelProxyConnection(ctx, request);
            } else {
                handleHttpProxyConnection(ctx, request);
            }
        } else {
            LOGGER.debug("{} : {}", connectionContext, request);
            connectionContext.serverChannel().writeAndFlush(ReferenceCountUtil.retain(request));
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

    private void handleTunnelProxyConnection(ChannelHandlerContext ctx,
                                             FullHttpRequest request) throws Exception {
        Address address = Address.resolve(request.uri());
        connectionContext.connect(address, ctx).addListener((future) -> {
            if (!future.isSuccess()) {
                ctx.close();
            }
        });

        FullHttpResponse response =
                new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
        LOGGER.debug("{} : {}", connectionContext,
                     response.getClass().getSimpleName());
        ctx.writeAndFlush(response);
        ctx.pipeline().replace(Http1FrontendHandler.this, null,
                connectionContext.provider().tlsFrontendHandler());
    }

    private void handleHttpProxyConnection(ChannelHandlerContext ctx,
                                           FullHttpRequest request) throws Exception {
        HttpUrl httpUrl = HttpUrl.resolve(request.uri());
        Address address = new Address(httpUrl.getHost(), httpUrl.getPort());
        FullHttpRequest newRequest = request.copy();
        connectionContext.connect(address, ctx).addListener((ChannelFuture future) -> {
            if (future.isSuccess()) {
                newRequest.headers().set(request.headers());
                newRequest.setUri(httpUrl.getPath());

                LOGGER.debug("{} : {}", connectionContext, newRequest);
                future.channel().writeAndFlush(newRequest);
            } else {
                newRequest.release();
                ctx.channel().close();
            }
        });
        if (!connectionContext.tlsCtx().isNegotiated()) {
            connectionContext.tlsCtx().disableTls();
        }
    }
}
