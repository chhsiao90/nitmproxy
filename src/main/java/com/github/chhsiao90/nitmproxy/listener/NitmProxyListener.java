package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.event.ForwardEvent;
import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FrameWrapper;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FramesWrapper;
import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.List;
import java.util.Optional;

public interface NitmProxyListener {

    default void onHttpEvent(HttpEvent event) {
    }

    default Optional<FullHttpResponse> onHttp1Request(ConnectionContext connectionContext, FullHttpRequest request) {
        return Optional.empty();
    }

    default List<HttpObject> onHttp1Response(ConnectionContext connectionContext, HttpObject response) {
        return ImmutableList.of(response);
    }

    default Optional<Http2FramesWrapper> onHttp2Request(ConnectionContext connectionContext,
            Http2FramesWrapper request) {
        return Optional.empty();
    }

    default List<Http2FrameWrapper<?>> onHttp2Response(ConnectionContext connectionContext,
            Http2FrameWrapper<?> frame) {
        return ImmutableList.of(frame);
    }

    default void onSendingWsFrame(ConnectionContext connectionContext, WebSocketFrame frame) {
    }

    default void onReceivingWsFrame(ConnectionContext connectionContext, WebSocketFrame frame) {
    }

    default void onForwardEvent(ConnectionContext connectionContext, ForwardEvent forwardEvent) {
    }

    default void onForwardRequest(ConnectionContext connectionContext, ByteBuf byteBuf) {
    }

    default void onForwardResponse(ConnectionContext connectionContext, ByteBuf byteBuf) {
    }

    default void close(ConnectionContext connectionContext) {
    }

    class Empty implements NitmProxyListener {
    }
}
