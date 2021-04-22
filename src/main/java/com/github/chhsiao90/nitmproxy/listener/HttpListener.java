package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FrameWrapper;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

public interface HttpListener {

    default void onHttpEvent(HttpEvent event) {
    }

    default void onHttp1Request(HttpRequest request) {
    }

    default void onHttp1RequestData(HttpContent data) {
    }

    default void onHttp1Response(HttpResponse response) {
    }

    default void onHttp1ResponseData(HttpContent data) {
    }

    default void onHttp2RequestFrame(Http2FrameWrapper<?> frame) {
    }

    default void onHttp2ResponseFrame(Http2FrameWrapper<?> frame) {
    }
}