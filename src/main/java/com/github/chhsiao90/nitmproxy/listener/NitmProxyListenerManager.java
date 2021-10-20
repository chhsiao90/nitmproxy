package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.event.ForwardEvent;
import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FrameWrapper;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FramesWrapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class NitmProxyListenerManager implements NitmProxyListener {

    private final List<NitmProxyListener> listeners;
    private final List<NitmProxyListener> reversedListeners;

    public NitmProxyListenerManager(List<NitmProxyListener> listeners) {
        this.listeners = ImmutableList.<NitmProxyListener>builder()
                                          .add(new HttpEventLogger())
                                          .addAll(listeners)
                                          .build();
        this.reversedListeners = Lists.reverse(this.listeners);
    }

    @Override
    public void onHttpEvent(HttpEvent event) {
        listeners.forEach(listener -> listener.onHttpEvent(event));
    }

    @Override
    public Optional<FullHttpResponse> onHttp1Request(ConnectionContext connectionContext, FullHttpRequest request) {
        Function<NitmProxyListener, Stream<FullHttpResponse>> apply = listener -> listener
                .onHttp1Request(connectionContext, request)
                .map(Stream::of)
                .orElse(Stream.empty());
        return listeners.stream().flatMap(apply).findFirst();
    }

    @Override
    public List<HttpObject> onHttp1Response(ConnectionContext connectionContext, HttpObject response) {
        return reversedListeners.stream()
                .reduce(ImmutableList.of(response),
                    (objects, listener) -> objects.stream()
                            .flatMap(f -> listener.onHttp1Response(connectionContext, f).stream())
                            .collect(ImmutableList.toImmutableList()),
                    (accu, objects) -> objects);
    }

    @Override
    public Optional<Http2FramesWrapper> onHttp2Request(ConnectionContext connectionContext,
                                                       Http2FramesWrapper request) {
        Function<NitmProxyListener, Stream<Http2FramesWrapper>> apply = listener -> listener
                .onHttp2Request(connectionContext, request)
                .map(Stream::of)
                .orElse(Stream.empty());
        return listeners.stream().flatMap(apply).findFirst();
    }

    @Override
    public List<Http2FrameWrapper<?>> onHttp2Response(ConnectionContext connectionContext, Http2FrameWrapper<?> frame) {
        return reversedListeners.stream()
                .reduce(ImmutableList.of(frame),
                    (frames, listener) -> frames.stream()
                            .flatMap(f -> listener.onHttp2Response(connectionContext, f).stream())
                            .collect(ImmutableList.toImmutableList()),
                    (accu, frames) -> frames);
    }

    @Override
    public void onSendingWsFrame(ConnectionContext connectionContext, WebSocketFrame frame) {
        listeners.forEach(listener -> listener.onSendingWsFrame(connectionContext, frame));
    }

    @Override
    public void onReceivingWsFrame(ConnectionContext connectionContext, WebSocketFrame frame) {
        listeners.forEach(listener -> listener.onReceivingWsFrame(connectionContext, frame));
    }

    @Override
    public void onForwardEvent(ConnectionContext connectionContext, ForwardEvent event) {
        listeners.forEach(listener -> listener.onForwardEvent(connectionContext, event));
    }

    @Override
    public void onForwardRequest(ConnectionContext connectionContext, ByteBuf byteBuf) {
        listeners.forEach(listener -> listener.onForwardRequest(connectionContext, byteBuf));
    }

    @Override
    public void onForwardResponse(ConnectionContext connectionContext, ByteBuf byteBuf) {
        reversedListeners.forEach(listener -> listener.onForwardResponse(connectionContext, byteBuf));
    }
}
