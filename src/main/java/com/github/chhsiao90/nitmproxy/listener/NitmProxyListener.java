package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.event.ForwardEvent;
import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FrameWrapper;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FramesWrapper;
import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.List;
import java.util.Optional;

public interface NitmProxyListener {

    /**
     * This callback will be invoked when the client channel was first initialized.
     *
     * @param connectionContext the connection context
     * @param clientChannel the client channel
     */
    default void onInit(ConnectionContext connectionContext, Channel clientChannel) {
    }

    /**
     * This callback will be invoked when the server channel was connected.
     *
     * @param connectionContext the connection context
     * @param serverChannel the server channel
     */
    default void onConnect(ConnectionContext connectionContext, Channel serverChannel) {
    }

    /**
     * This callback will be invoked when a full request and response was served.
     *
     * @param event the http event
     */
    default void onHttpEvent(HttpEvent event) {
    }

    /**
     * This callback will be invoked when receiving a request from client.
     *
     * @param connectionContext the connection context
     * @param request the request
     * @return response if you want to intercept the request
     */
    default Optional<FullHttpResponse> onHttp1Request(ConnectionContext connectionContext, FullHttpRequest request) {
        return Optional.empty();
    }

    /**
     * This callback will be invoked when receiving a response from server.
     *
     * @param connectionContext the connection context
     * @param response the response
     * @return intercepted response objects, or you should send a list containing only the origin response
     */
    default List<HttpObject> onHttp1Response(ConnectionContext connectionContext, HttpObject response) {
        return ImmutableList.of(response);
    }

    /**
     * This callback will be invoked when receiving a request from client.
     *
     * @param connectionContext the connection context
     * @param request the request
     * @return response if you want to intercept the request
     */
    default Optional<Http2FramesWrapper> onHttp2Request(ConnectionContext connectionContext,
            Http2FramesWrapper request) {
        return Optional.empty();
    }

    /**
     * This callback will be invoked when receiving a response from server.
     *
     * @param connectionContext the connection context
     * @param frame the response frame
     * @return intercepted response objects, or you should send a list containing only the origin response
     */
    default List<Http2FrameWrapper<?>> onHttp2Response(ConnectionContext connectionContext,
            Http2FrameWrapper<?> frame) {
        return ImmutableList.of(frame);
    }

    /**
     * This callback will be invoked while receiving a ws request from client.
     *
     * @param connectionContext the connection context
     * @param frame the ws frame
     */
    default void onWsRequest(ConnectionContext connectionContext, WebSocketFrame frame) {
    }

    /**
     * This callback will be invoked while receiving a ws response from server.
     *
     * @param connectionContext the connection context
     * @param frame the ws frame
     */
    default void onWsResponse(ConnectionContext connectionContext, WebSocketFrame frame) {
    }

    /**
     * This callback will be invoked while receiving a ws response from server.
     *
     * @param connectionContext the connection context
     * @param forwardEvent the forward event
     */
    default void onForwardEvent(ConnectionContext connectionContext, ForwardEvent forwardEvent) {
    }

    /**
     * This callback will be invoked while receiving data from client.
     *
     * @param connectionContext the connection context
     * @param data the data
     */
    default void onForwardRequest(ConnectionContext connectionContext, ByteBuf data) {
    }

    /**
     * This callback will be invoked while receiving data from server.
     *
     * @param connectionContext the connection context
     * @param data the data
     */
    default void onForwardResponse(ConnectionContext connectionContext, ByteBuf data) {
    }

    /**
     * This callback will be called after the channel was closed, you should release objects if needed.
     *
     * @param connectionContext the connection context
     */
    default void close(ConnectionContext connectionContext) {
    }

    class Empty implements NitmProxyListener {
    }
}
