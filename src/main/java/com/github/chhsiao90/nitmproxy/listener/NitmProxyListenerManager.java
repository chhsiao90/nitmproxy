package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.event.ForwardEvent;
import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FrameWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;

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
    public void onHttp1Request(HttpRequest request) {
        httpListeners.forEach(listener -> listener.onHttp1Request(request));
    }

    @Override
    public void onHttp1RequestData(HttpContent data) {
        httpListeners.forEach(listener -> listener.onHttp1RequestData(data));
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
    public void onHttp2RequestFrame(Http2FrameWrapper<?> frame) {
        httpListeners.forEach(listener -> listener.onHttp2RequestFrame(frame));
    }

    @Override
    public void onHttp2ResponseFrame(Http2FrameWrapper<?> frame) {
        httpListeners.forEach(listener -> listener.onHttp2ResponseFrame(frame));
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
