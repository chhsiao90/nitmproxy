package com.github.chhsiao.nitm.nitmproxy.layer.protocol.http1;

import com.github.chhsiao.nitm.nitmproxy.Address;
import com.github.chhsiao.nitm.nitmproxy.ConnectionInfo;
import com.github.chhsiao.nitm.nitmproxy.NitmProxyConfig;
import com.github.chhsiao.nitm.nitmproxy.ProxyMode;
import com.google.common.base.Strings;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.*;

public class Http1FrontendHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final Pattern PATH_PATTERN = Pattern.compile("(https?)://([a-zA-Z0-9\\.\\-]+)(:(\\d+))?(/.*)");

    private static final Logger LOGGER = LoggerFactory.getLogger(Http1FrontendHandler.class);

    private NitmProxyConfig config;
    private ConnectionInfo connectionInfo;

    private Channel outboundChannel;
    private ChannelHandler httpServerCodec;

    public Http1FrontendHandler(NitmProxyConfig config, ConnectionInfo connectionInfo) {
        this(config, connectionInfo, null);
    }

    public Http1FrontendHandler(NitmProxyConfig config, ConnectionInfo connectionInfo, Channel outboundChannel) {
        super();
        this.config = config;
        this.connectionInfo = connectionInfo;
        this.outboundChannel = outboundChannel;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : Http1FrontendHandler handlerAdded", connectionInfo);

        httpServerCodec = new HttpServerCodec();
        ctx.pipeline().addBefore(ctx.name(), null, httpServerCodec);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : Http1FrontendHandler handlerRemoved", connectionInfo);

        ctx.pipeline().remove(httpServerCodec);

        if (outboundChannel != null) {
            outboundChannel.close();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                HttpObject httpObject) throws Exception {
        if (config.getProxyMode() == ProxyMode.HTTP && httpObject instanceof HttpRequest) {
            handleHttpProxyConnection(ctx, (HttpRequest) httpObject);
        } else {
            checkState(outboundChannel != null);
            LOGGER.info("[Client ({})] => [Server ({})] : {}",
                        connectionInfo.getClientAddr(), connectionInfo.getServerAddr(),
                        httpObject);
            outboundChannel.writeAndFlush(ReferenceCountUtil.retain(httpObject));
        }
    }

    private void handleHttpProxyConnection(ChannelHandlerContext ctx,
                                           HttpRequest request) throws Exception {
        FullPath fullPath = resolveFullPath(request.uri());

        if (request.method() == HttpMethod.CONNECT) {
            HttpResponse response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
            LOGGER.info("[Client ({})] <= [Proxy] : {}", connectionInfo.getClientAddr(), response);
            ctx.writeAndFlush(response);

            ConnectionInfo newConnInfo = new ConnectionInfo(
                    connectionInfo.getClientAddr(),
                    new Address(fullPath.host, fullPath.port));
            Http1FrontendHandler connectedHandler = new Http1FrontendHandler(config, newConnInfo);
            ctx.pipeline().replace(this, null, connectedHandler);
        } else {
            createOrGetOutboundChannel(
                    ctx, new Address(fullPath.host, fullPath.port));
            HttpRequest newRequest = new DefaultHttpRequest(
                    request.protocolVersion(), request.method(), fullPath.path, request.headers());

            LOGGER.info("[Client ({})] => [Server ({})] : {}",
                        connectionInfo.getClientAddr(), connectionInfo.getServerAddr(),
                        newRequest);
            outboundChannel.writeAndFlush(newRequest);
        }
    }

    private FullPath resolveFullPath(String fullPath) {
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

    private int resolvePort(String scheme, String port) {
        if (Strings.isNullOrEmpty(port)) {
            return "https".equals(scheme) ? 443 : 80;
        }
        return Integer.parseInt(port);
    }

    private void createOrGetOutboundChannel(ChannelHandlerContext ctx, Address serverAddr) throws InterruptedException {
        if (outboundChannel != null) {
            if (connectionInfo.getServerAddr().equals(serverAddr)) {
                return;
            }
            outboundChannel.close();
            outboundChannel = null;
        }

        connectionInfo = new ConnectionInfo(connectionInfo.getClientAddr(),
                                            new Address(serverAddr.getHost(), serverAddr.getPort()));
        Bootstrap bootstrap = new Bootstrap()
                .group(ctx.channel().eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new Http1BackendHandler(config, connectionInfo, ctx.channel()));
        ChannelFuture future = bootstrap.connect(serverAddr.getHost(), serverAddr.getPort());
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    ctx.channel().close();
                }
            }
        });

        outboundChannel = future.channel();
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
