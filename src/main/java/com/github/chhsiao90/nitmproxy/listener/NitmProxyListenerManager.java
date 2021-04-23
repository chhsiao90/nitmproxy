package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FrameWrapper;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;

public class NitmProxyListenerManager implements HttpListener {

    private List<HttpListener> httpListeners = new ArrayList<>();

    public NitmProxyListenerManager(List<HttpListener> listeners) {
        httpListeners.add(new HttpEventLogger());
        httpListeners.addAll(listeners);
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
}
