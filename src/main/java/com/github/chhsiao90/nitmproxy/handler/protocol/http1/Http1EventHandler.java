package com.github.chhsiao90.nitmproxy.handler.protocol.http1;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.listener.HttpEventListener;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;

import java.util.concurrent.atomic.AtomicLong;

import static com.github.chhsiao90.nitmproxy.http.HttpHeadersUtil.*;
import static com.google.common.base.Preconditions.*;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.util.ReferenceCountUtil.*;
import static java.lang.System.*;

public class Http1EventHandler extends ChannelDuplexHandler {

    private HttpEventListener listener;
    private ConnectionContext connectionContext;

    private long requestTime;
    private FullHttpRequest request;
    private HttpResponse response;
    private AtomicLong responseBytes;

    /**
     * Create new instance of http1 event handler.
     *
     * @param master            the master
     * @param connectionContext the connection context
     */
    public Http1EventHandler(
            NitmProxyMaster master,
            ConnectionContext connectionContext) {
        this.listener = master.httpEventListener();
        this.connectionContext = connectionContext;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
            throws Exception {
        if (msg instanceof HttpResponse) {
            checkState(request != null, "request is null");
            checkState(response == null, "response is not null");
            responseBytes = new AtomicLong();
            response = retain((HttpResponse) msg);
        }
        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            responseBytes.addAndGet(httpContent.content().readableBytes());
        }
        if (msg instanceof LastHttpContent) {
            checkState(request != null, "request is null");
            checkState(response != null, "response is null");
            long responseTime = currentTimeMillis();
            HttpEvent httpEvent = HttpEvent.builder(connectionContext)
                                           .method(request.method())
                                           .version(request.protocolVersion())
                                           .host(request.headers().get(HOST))
                                           .path(request.uri())
                                           .requestBodySize(request.content().readableBytes())
                                           .requestTime(requestTime)
                                           .status(response.status())
                                           .contentType(getContentType(response.headers()))
                                           .responseTime(responseTime)
                                           .responseBodySize(responseBytes.get())
                                           .build();

            try {
                listener.onHttpEvent(httpEvent);
            } finally {
                release(request);
                release(response);
                request = null;
                requestTime = 0;
                response = null;
                responseBytes = null;
            }
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            checkState(request == null, "request is not null");
            checkState(response == null, "response is not null");
            request = (FullHttpRequest) retain(msg);
            requestTime = currentTimeMillis();
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        release(request);
        release(response);
        ctx.fireChannelInactive();
    }
}
