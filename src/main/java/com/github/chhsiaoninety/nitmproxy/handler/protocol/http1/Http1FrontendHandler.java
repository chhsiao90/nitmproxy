package com.github.chhsiaoninety.nitmproxy.handler.protocol.http1;

import com.github.chhsiaoninety.nitmproxy.Address;
import com.github.chhsiaoninety.nitmproxy.ConnectionContext;
import com.github.chhsiaoninety.nitmproxy.NitmProxyMaster;
import com.github.chhsiaoninety.nitmproxy.enums.Handler;
import com.github.chhsiaoninety.nitmproxy.enums.ProxyMode;
import com.github.chhsiaoninety.nitmproxy.event.OutboundChannelClosedEvent;
import com.google.common.base.Strings;
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
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Http1FrontendHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Pattern PATH_PATTERN = Pattern.compile("(https?)://([a-zA-Z0-9\\.\\-]+)(:(\\d+))?(/.*)");
    private static final Pattern TUNNEL_ADDR_PATTERN = Pattern.compile("^([a-zA-Z0-9\\.\\-_]+):(\\d+)");

    private static final Logger LOGGER = LoggerFactory.getLogger(Http1FrontendHandler.class);

    private NitmProxyMaster master;
    private ConnectionContext connectionContext;
    private boolean tunneled;

    private ChannelHandler httpServerCodec;
    private ChannelHandler httpObjectAggregator;

    public Http1FrontendHandler(NitmProxyMaster master, ConnectionContext connectionContext) {
        super();
        this.master = master;
        this.connectionContext = connectionContext;
        this.tunneled = connectionContext.connected();

    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : handlerAdded", connectionContext);

        httpServerCodec = new HttpServerCodec();
        httpObjectAggregator = new HttpObjectAggregator(master.config().getMaxContentLength());
        ctx.pipeline()
           .addBefore(ctx.name(), null, httpServerCodec)
           .addBefore(ctx.name(), null, httpObjectAggregator);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : handlerRemoved", connectionContext);

        ctx.pipeline().remove(httpServerCodec).remove(httpObjectAggregator);

        if (tunneled) {
            connectionContext.serverChannel().close();
        }
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
            LOGGER.info("[Client ({})] => [Server ({})] : {}",
                        connectionContext.getClientAddr(), connectionContext.getServerAddr(),
                        request);
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
        Address address = resolveTunnelAddr(request.uri());
        HttpVersion protocolVersion = request.protocolVersion();
        createOutboundChannel(ctx, address).addListener((future) -> {
            if (future.isSuccess()) {
                FullHttpResponse response =
                        new DefaultFullHttpResponse(protocolVersion, HttpResponseStatus.OK);
                LOGGER.info("[Client ({})] <= [Proxy] : {}", connectionContext.getClientAddr(), response);
                ctx.writeAndFlush(response);
                ctx.pipeline().replace(Http1FrontendHandler.this, null,
                        connectionContext.handler(Handler.TLS_FRONTEND));
            }
        });
    }

    private void handleHttpProxyConnection(ChannelHandlerContext ctx,
                                           FullHttpRequest request) throws Exception {
        FullPath fullPath = resolveHttpProxyPath(request.uri());
        Address serverAddr = new Address(fullPath.host, fullPath.port);
        connectionContext.connect(serverAddr, ctx).addListener((ChannelFuture future) -> {
           if (future.isSuccess()) {
               FullHttpRequest newRequest = request.copy();
               newRequest.headers().set(request.headers());
               newRequest.setUri(fullPath.path);

               LOGGER.info("[Client ({})] => [Server ({})] : {}",
                       connectionContext.getClientAddr(), connectionContext.getServerAddr(),
                       newRequest);
               future.channel().writeAndFlush(newRequest);
           } else {
               ctx.channel().close();
           }
        });
    }

    private FullPath resolveHttpProxyPath(String fullPath) {
        Matcher matcher = PATH_PATTERN.matcher(fullPath);
        if (matcher.find()) {
            String scheme = matcher.group(1);
            String host = matcher.group(2);
            int port = resolvePort(scheme, matcher.group(4));
            String path = matcher.group(5);
            return new FullPath(scheme, host, port, path);
        } else {
            throw new IllegalStateException("Illegal http proxy path: " + fullPath);
        }
    }

    private Address resolveTunnelAddr(String addr) {
        Matcher matcher = TUNNEL_ADDR_PATTERN.matcher(addr);
        if (matcher.find()) {
            return new Address(matcher.group(1), Integer.parseInt(matcher.group(2)));
        } else {
            throw new IllegalStateException("Illegal tunnel addr: " + addr);
        }
    }

    private int resolvePort(String scheme, String port) {
        if (Strings.isNullOrEmpty(port)) {
            return "https".equals(scheme) ? 443 : 80;
        }
        return Integer.parseInt(port);
    }

    private ChannelFuture createOutboundChannel(ChannelHandlerContext ctx, Address serverAddr) {
        ChannelFuture future = connectionContext.connect(serverAddr, ctx);
        future.addListener((f) -> {
            if (!f.isSuccess()) {
                ctx.channel().close();
            }
        });
        return future;
    }

    private static class FullPath {
        private String scheme;
        private String host;
        private int port;
        private String path;

        private FullPath(String scheme, String host, int port, String path) {
            this.scheme = scheme;
            this.host = host;
            this.port = port;
            this.path = path;
        }

        @Override
        public String toString() {
            return "FullPath{" +
                   "scheme='" + scheme + '\'' +
                   ", host='" + host + '\'' +
                   ", port=" + port +
                   ", path='" + path + '\'' +
                   '}';
        }
    }
}
