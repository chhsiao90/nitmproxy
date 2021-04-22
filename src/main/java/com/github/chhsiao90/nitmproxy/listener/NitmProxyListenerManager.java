package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FrameWrapper;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;

public class NitmProxyListenerManager implements HttpListener {

    private List<HttpListener> httpEventListeners = new ArrayList<>();

    public NitmProxyListenerManager() {
        httpEventListeners.add(new HttpEventLogger());
    }

    @Override
    public void onHttpEvent(HttpEvent event) {
        httpEventListeners.forEach(listener -> listener.onHttpEvent(event));
    }

    @Override
    public void onHttp1Request(HttpRequest request) {
        httpEventListeners.forEach(listener -> listener.onHttp1Request(request));
    }

    @Override
    public void onHttp1RequestData(HttpContent data) {
        httpEventListeners.forEach(listener -> listener.onHttp1RequestData(data));
    }

    @Override
    public void onHttp1Response(HttpResponse response) {
        httpEventListeners.forEach(listener -> listener.onHttp1Response(response));
    }

    @Override
    public void onHttp1ResponseData(HttpContent data) {
        httpEventListeners.forEach(listener -> listener.onHttp1ResponseData(data));
    }

    @Override
    public void onHttp2RequestFrame(Http2FrameWrapper<?> frame) {
        httpEventListeners.forEach(listener -> listener.onHttp2RequestFrame(frame));
    }

    @Override
    public void onHttp2ResponseFrame(Http2FrameWrapper<?> frame) {
        httpEventListeners.forEach(listener -> listener.onHttp2ResponseFrame(frame));
    }
}
