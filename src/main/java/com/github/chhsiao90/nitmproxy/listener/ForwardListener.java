package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.event.ForwardEvent;
import io.netty.buffer.ByteBuf;

public interface ForwardListener {

    default void onForwardEvent(ForwardEvent forwardEvent) {
    }

    default void onForwardRequest(ByteBuf byteBuf) {
    }

    default void onForwardResponse(ByteBuf byteBuf) {
    }

}