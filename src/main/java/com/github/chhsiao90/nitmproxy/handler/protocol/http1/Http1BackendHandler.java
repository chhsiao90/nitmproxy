package com.github.chhsiao90.nitmproxy.handler.protocol.http1;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.event.OutboundChannelClosedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.ReferenceCountUtil;

public class Http1BackendHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Http1BackendHandler.class);

    private NitmProxyMaster master;

    private ConnectionContext connectionContext;

    private DelayOutboundHandler delayOutboundHandler;

    private volatile HttpRequest currentRequest;

    private static class DataLogger extends ChannelDuplexHandler {

        private final ConnectionContext connectionContext;

        public DataLogger(ConnectionContext connectionContext) {
            this.connectionContext = connectionContext;
        }

        private void logBytes(ByteBuf byteBuf, Consumer<byte[]> writer) {
            byte[] bytes = ByteBufUtil.getBytes(byteBuf.duplicate());
            writer.accept(bytes);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (connectionContext.config().getResponseLogger() != null) {
                logBytes(((ByteBuf) msg), connectionContext.config().getResponseLogger());
            }
            super.channelRead(ctx, msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (connectionContext.config().getRequestLogger() != null) {
                logBytes(((ByteBuf) msg), connectionContext.config().getRequestLogger());
            }
            super.write(ctx, msg, promise);
        }

    }

    public Http1BackendHandler(NitmProxyMaster master, ConnectionContext connectionContext) {
        this.master = master;
        this.connectionContext = connectionContext;

        delayOutboundHandler = new DelayOutboundHandler();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : channelActive", connectionContext);
        delayOutboundHandler.next();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : channelInactive", connectionContext);
        delayOutboundHandler.release();
        connectionContext.clientChannel().pipeline().fireUserEventTriggered(new OutboundChannelClosedEvent(connectionContext, false));
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : handlerAdded", connectionContext);

        ctx.pipeline()
           .addBefore(ctx.name(), null, new DataLogger(connectionContext))
           .addBefore(ctx.name(), null, new HttpClientCodec())
           .addBefore(ctx.name(), null, delayOutboundHandler);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, HttpObject httpObject)
            throws Exception {
        LOGGER.info("[Client ({})] <= [Server ({})] : {}",
                    connectionContext.getClientAddr(), connectionContext.getServerAddr(),
                    httpObject);

        connectionContext.clientChannel().writeAndFlush(ReferenceCountUtil.retain(httpObject));

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
            if (msg instanceof FullHttpRequest) {
                LOGGER.info("[Client ({})] => [Server ({})] : (PENDING) {}",
                            connectionContext.getClientAddr(), connectionContext.getServerAddr(),
                            msg);
                HttpRequest request = (HttpRequest) msg;
                pendings.offer(new RequestPromise(request, promise));
                next();
            } else if (msg instanceof HttpObject) {
                throw new IllegalStateException("Cannot handled message: " + msg.getClass());
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
                        connectionContext.getClientAddr(), connectionContext.getServerAddr(),
                        requestPromise.request);

            thisCtx.writeAndFlush(requestPromise.request, requestPromise.promise);
        }

        private void release() {
            while (!pendings.isEmpty()) {
                RequestPromise requestPromise = pendings.poll();
                LOGGER.info("{} : {} is dropped", connectionContext, requestPromise.request);
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
