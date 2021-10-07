package com.github.chhsiao90.nitmproxy.handler.protocol.ws;

import io.netty.handler.codec.http.websocketx.extensions.WebSocketServerExtensionHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.DeflateFrameServerExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.compression.PerMessageDeflateServerExtensionHandshaker;

public class WebSocketServerCompressionHandler extends WebSocketServerExtensionHandler {
    public WebSocketServerCompressionHandler() {
        super(new PerMessageDeflateServerExtensionHandshaker(),
                new DeflateFrameServerExtensionHandshaker());
    }
}
