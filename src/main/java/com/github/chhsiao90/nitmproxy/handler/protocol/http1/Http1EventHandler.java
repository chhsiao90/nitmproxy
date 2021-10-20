package com.github.chhsiao90.nitmproxy.handler.protocol.http1;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.listener.NitmProxyListener;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.PromiseCombiner;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.chhsiao90.nitmproxy.http.HttpHeadersUtil.*;
import static com.google.common.base.Preconditions.*;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.util.ReferenceCountUtil.*;
import static java.lang.System.*;

public class Http1EventHandler extends ChannelDuplexHandler {

    private NitmProxyListener listener;
    private ConnectionContext connectionContext;

    private long requestTime;
    private Queue<FullHttpRequest> requests;
    private HttpResponse response;
    private AtomicLong responseBytes;

    /**
     * Create new instance of http1 event handler.
     *
     * @param master            the master
     * @param connectionContext the connection context
     */
    public Http1EventHandler(NitmProxyMaster master, ConnectionContext connectionContext) {
        this.listener = master.createListener();
        this.connectionContext = connectionContext;
        this.requests = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
            throws Exception {
        if (!(msg instanceof HttpObject)) {
            ctx.write(msg, promise);
            return;
        }
        List<HttpObject> output = listener.onHttp1Response(connectionContext, (HttpObject) msg);
        for (HttpObject httpObject : output) {
            if (httpObject instanceof HttpResponse) {
                checkState(!requests.isEmpty(), "request is empty");
                checkState(response == null, "response is not null");
                responseBytes = new AtomicLong();
                response = retain((HttpResponse) httpObject);
            }
            if (httpObject instanceof HttpContent) {
                checkState(responseBytes != null, "responseBytes is null");
                HttpContent httpContent = (HttpContent) msg;
                responseBytes.addAndGet(httpContent.content().readableBytes());
            }
            if (httpObject instanceof LastHttpContent) {
                checkState(!requests.isEmpty(), "request is empty");
                checkState(response != null, "response is null");
                FullHttpRequest request = requests.poll();
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
                    requestTime = 0;
                    response = null;
                    responseBytes = null;
                }
            }
        }

        if (output.isEmpty()) {
            ctx.write(msg, promise);
        } else if (output.size() == 1) {
            ctx.write(output.get(0), promise);
        } else {
            PromiseCombiner combiner = new PromiseCombiner(ctx.executor());
            output.stream().map(ctx::write).forEach(combiner::add);
            combiner.finish(promise);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof FullHttpRequest)) {
            ctx.fireChannelRead(msg);
            return;
        }

        FullHttpRequest request = (FullHttpRequest) msg;
        Optional<FullHttpResponse> response =  listener.onHttp1Request(connectionContext, (FullHttpRequest) msg);
        if (response.isPresent()) {
            try {
                sendResponse(ctx, request, response.get());
            } finally {
                request.release();
            }
            return;
        }

        this.requests.add(request.retain());
        this.requestTime = currentTimeMillis();
        ctx.fireChannelRead(msg);
    }

    private void sendResponse(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse response) {
        HttpEvent httpEvent = HttpEvent.builder(connectionContext)
                .method(request.method())
                .version(request.protocolVersion())
                .host(request.headers().get(HOST))
                .path(request.uri())
                .requestBodySize(request.content().readableBytes())
                .requestTime(currentTimeMillis())
                .status(response.status())
                .contentType(getContentType(response.headers()))
                .responseBodySize(response.content().readableBytes())
                .build();
        listener.onHttpEvent(httpEvent);
        ctx.writeAndFlush(response);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        requests.forEach(FullHttpRequest::release);
        release(response);
        listener.close(connectionContext);
    }
}
