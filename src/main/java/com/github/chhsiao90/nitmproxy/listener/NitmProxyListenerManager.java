package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.event.ForwardEvent;
import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2DataFrameWrapper;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FrameWrapper;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FramesWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http2.Http2HeadersFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class NitmProxyListenerManager implements HttpListener, ForwardListener {

    private final List<HttpListener> httpListeners = new ArrayList<>();
    private final List<ForwardListener> forwardListeners = new ArrayList<>();

    public NitmProxyListenerManager(List<HttpListener> httpListeners,
                                    List<ForwardListener> forwardListeners) {
        this.httpListeners.add(new HttpEventLogger());
        this.httpListeners.addAll(httpListeners);
        this.forwardListeners.add(new ForwardEventLogger());
        this.forwardListeners.addAll(forwardListeners);
    }

    @Override
    public void onHttpEvent(HttpEvent event) {
        httpListeners.forEach(listener -> listener.onHttpEvent(event));
    }

    @Override
    public Optional<FullHttpResponse> onHttp1Request(FullHttpRequest request) {
        Function<HttpListener, Stream<FullHttpResponse>> apply = listener -> listener.onHttp1Request(request)
                .map(Stream::of)
                .orElse(Stream.empty());
        return httpListeners.stream().flatMap(apply).findFirst();
    }

    @Override
    public void onHttp1Response(HttpResponse response) {
        httpListeners.forEach(listener -> listener.onHttp1Response(response));
    }

    @Override
    public void onHttp1ResponseData(HttpContent data) {
        httpListeners.forEach(listener -> listener.onHttp1ResponseData(data));
    }

    @Override
    public Optional<Http2FramesWrapper> onHttp2Request(Http2FramesWrapper request) {
        Function<HttpListener, Stream<Http2FramesWrapper>> apply = listener -> listener
                .onHttp2Request(request)
                .map(Stream::of)
                .orElse(Stream.empty());
        return httpListeners.stream().flatMap(apply).findFirst();
    }

    @Override
    public void onHttp2Response(Http2FrameWrapper<Http2HeadersFrame> frame) {
        httpListeners.forEach(listener -> listener.onHttp2Response(frame));
    }

    @Override
    public void onHttp2ResponseData(Http2DataFrameWrapper frame) {
        httpListeners.forEach(listener -> listener.onHttp2ResponseData(frame));
    }

    @Override
    public void onForwardEvent(ForwardEvent event) {
        forwardListeners.forEach(listener -> listener.onForwardEvent(event));
    }

    @Override
    public void onForwardRequest(ByteBuf byteBuf) {
        forwardListeners.forEach(listener -> listener.onForwardRequest(byteBuf));
    }

    @Override
    public void onForwardResponse(ByteBuf byteBuf) {
        forwardListeners.forEach(listener -> listener.onForwardResponse(byteBuf));
    }
}
