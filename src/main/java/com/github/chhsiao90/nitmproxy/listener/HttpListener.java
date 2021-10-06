package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2DataFrameWrapper;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FrameWrapper;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FramesWrapper;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;

import java.util.Optional;

public interface HttpListener {

    default void onHttpEvent(HttpEvent event) {
    }

    default Optional<FullHttpResponse> onHttp1Request(ConnectionContext connectionContext, FullHttpRequest request) {
        return Optional.empty();
    }

    default void onHttp1Response(ConnectionContext connectionContext, HttpResponse response) {
    }

    default void onHttp1ResponseData(ConnectionContext connectionContext, HttpContent data) {
    }

    default Optional<Http2FramesWrapper> onHttp2Request(ConnectionContext connectionContext,
                                                        Http2FramesWrapper request) {
        return Optional.empty();
    }

    default void onHttp2Response(ConnectionContext connectionContext, Http2FrameWrapper<Http2HeadersFrame> frame) {
    }

    default void onHttp2ResponseData(ConnectionContext connectionContext, Http2DataFrameWrapper frame) {
    }

    default void onSendingWsFrame(ConnectionContext connectionContext, WebSocketFrame frame) {
    }

    default void onReceivingWsFrame(ConnectionContext connectionContext, WebSocketFrame frame) {
    }
}