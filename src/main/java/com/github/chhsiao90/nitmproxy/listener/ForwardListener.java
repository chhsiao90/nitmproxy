package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.event.ForwardEvent;
import io.netty.buffer.ByteBuf;

public interface ForwardListener {

    default void onForwardEvent(ConnectionContext connectionContext, ForwardEvent forwardEvent) {
    }

    default void onForwardRequest(ConnectionContext connectionContext, ByteBuf byteBuf) {
    }

    default void onForwardResponse(ConnectionContext connectionContext, ByteBuf byteBuf) {
    }

}