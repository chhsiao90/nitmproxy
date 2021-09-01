package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.event.ForwardEvent;
import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2DataFrameWrapper;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FrameWrapper;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FramesWrapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http2.Http2HeadersFrame;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class NitmProxyListenerManager implements HttpListener, ForwardListener {

    private final List<HttpListener> httpListeners;
    private final List<HttpListener> reversedHttpListeners;
    private final List<ForwardListener> forwardListeners;
    private final List<ForwardListener> reversedForwardListeners;

    public NitmProxyListenerManager(List<HttpListener> httpListeners,
                                    List<ForwardListener> forwardListeners) {
        this.httpListeners = ImmutableList.<HttpListener>builder()
                                          .add(new HttpEventLogger())
                                          .addAll(httpListeners)
                                          .build();
        this.reversedHttpListeners = Lists.reverse(this.httpListeners);
        this.forwardListeners = ImmutableList.<ForwardListener>builder()
                                             .add(new ForwardEventLogger())
                                             .addAll(forwardListeners)
                                             .build();
        this.reversedForwardListeners = Lists.reverse(this.forwardListeners);
    }

    @Override
    public void onHttpEvent(HttpEvent event) {
        httpListeners.forEach(listener -> listener.onHttpEvent(event));
    }

    @Override
    public Optional<FullHttpResponse> onHttp1Request(ConnectionContext connectionContext, FullHttpRequest request) {
        Function<HttpListener, Stream<FullHttpResponse>> apply = listener -> listener.onHttp1Request(connectionContext,
                request)
                .map(Stream::of)
                .orElse(Stream.empty());
        return httpListeners.stream().flatMap(apply).findFirst();
    }

    @Override
    public void onHttp1Response(ConnectionContext connectionContext, HttpResponse response) {
        reversedHttpListeners.forEach(listener -> listener.onHttp1Response(connectionContext, response));
    }

    @Override
    public void onHttp1ResponseData(ConnectionContext connectionContext, HttpContent data) {
        reversedHttpListeners.forEach(listener -> listener.onHttp1ResponseData(connectionContext, data));
    }

    @Override
    public Optional<Http2FramesWrapper> onHttp2Request(ConnectionContext connectionContext,
                                                       Http2FramesWrapper request) {
        Function<HttpListener, Stream<Http2FramesWrapper>> apply = listener -> listener
                .onHttp2Request(connectionContext, request)
                .map(Stream::of)
                .orElse(Stream.empty());
        return httpListeners.stream().flatMap(apply).findFirst();
    }

    @Override
    public void onHttp2Response(ConnectionContext connectionContext, Http2FrameWrapper<Http2HeadersFrame> frame) {
        reversedHttpListeners.forEach(listener -> listener.onHttp2Response(connectionContext, frame));
    }

    @Override
    public void onHttp2ResponseData(ConnectionContext connectionContext, Http2DataFrameWrapper frame) {
        reversedHttpListeners.forEach(listener -> listener.onHttp2ResponseData(connectionContext, frame));
    }

    @Override
    public void onForwardEvent(ConnectionContext connectionContext, ForwardEvent event) {
        forwardListeners.forEach(listener -> listener.onForwardEvent(connectionContext, event));
    }

    @Override
    public void onForwardRequest(ConnectionContext connectionContext, ByteBuf byteBuf) {
        forwardListeners.forEach(listener -> listener.onForwardRequest(connectionContext, byteBuf));
    }

    @Override
    public void onForwardResponse(ConnectionContext connectionContext, ByteBuf byteBuf) {
        reversedForwardListeners.forEach(listener -> listener.onForwardResponse(connectionContext, byteBuf));
    }
}
