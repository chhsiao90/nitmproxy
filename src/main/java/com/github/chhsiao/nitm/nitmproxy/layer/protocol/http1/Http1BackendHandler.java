package com.github.chhsiao.nitm.nitmproxy.layer.protocol.http1;

import com.github.chhsiao.nitm.nitmproxy.ConnectionInfo;
import com.github.chhsiao.nitm.nitmproxy.NitmProxyConfig;
import com.github.chhsiao.nitm.nitmproxy.event.OutboundChannelClosedEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Http1BackendHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Http1BackendHandler.class);

    private NitmProxyConfig config;
    private ConnectionInfo connectionInfo;
    private Channel outboundChannel;

    private DelayOutboundHandler delayOutboundHandler;

    private volatile HttpRequest currentRequest;

    public Http1BackendHandler(NitmProxyConfig config, ConnectionInfo connectionInfo,
                               Channel outboundChannel) {
        this.config = config;
        this.connectionInfo = connectionInfo;
        this.outboundChannel = outboundChannel;

        delayOutboundHandler = new DelayOutboundHandler();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : channelActive", connectionInfo);
        delayOutboundHandler.next();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : channelInactive", connectionInfo);
        delayOutboundHandler.release();
        outboundChannel.pipeline().fireUserEventTriggered(new OutboundChannelClosedEvent(connectionInfo, false));
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : handlerAdded", connectionInfo);

        ctx.pipeline()
           .addBefore(ctx.name(), null, new HttpClientCodec())
           .addBefore(ctx.name(), null, delayOutboundHandler);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, HttpObject httpObject)
            throws Exception {
        LOGGER.info("[Client ({})] <= [Server ({})] : {}",
                    connectionInfo.getClientAddr(), connectionInfo.getServerAddr(),
                    httpObject);

        outboundChannel.writeAndFlush(ReferenceCountUtil.retain(httpObject));

        if (httpObject instanceof HttpResponse) {
            currentRequest = null;
            delayOutboundHandler.next();
        }
    }

    private class DelayOutboundHandler extends ChannelOutboundHandlerAdapter {
        private Deque<RequestPromise> pendings = new ConcurrentLinkedDeque<>();
        private ChannelHandlerContext thisCtx;

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            thisCtx = ctx.pipeline().context(this);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof HttpRequest) {
                LOGGER.info("[Client ({})] => [Server ({})] : (PENDING) {}",
                            connectionInfo.getClientAddr(), connectionInfo.getServerAddr(),
                            msg);
                HttpRequest request = (HttpRequest) msg;
                pendings.push(new RequestPromise(ReferenceCountUtil.retain(request), promise));
                next();
            } else {
                ctx.write(msg, promise);
            }
        }

        private void next() {
            if (currentRequest != null || !thisCtx.channel().isActive() || pendings.isEmpty()) {
                return;
            }

            RequestPromise requestPromise = pendings.poll();
            currentRequest = requestPromise.request;
            LOGGER.info("[Client ({})] => [Server ({})] : {}",
                        connectionInfo.getClientAddr(), connectionInfo.getServerAddr(),
                        requestPromise.request);

            thisCtx.writeAndFlush(requestPromise.request, requestPromise.promise);
        }

        private void release() {
            while (!pendings.isEmpty()) {
                RequestPromise requestPromise = pendings.poll();
                LOGGER.info("{} : {} is dropped", connectionInfo.toString(true), requestPromise.request);
                requestPromise.promise.setFailure(new IOException("Cannot send request to server"));
                ReferenceCountUtil.release(requestPromise.request);
            }
        }
    }

    private static class RequestPromise {
        private HttpRequest request;
        private ChannelPromise promise;

        private RequestPromise(HttpRequest request, ChannelPromise promise) {
            this.request = request;
            this.promise = promise;
        }
    }
}
