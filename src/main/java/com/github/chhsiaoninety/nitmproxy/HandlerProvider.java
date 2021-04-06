package com.github.chhsiaoninety.nitmproxy;

import com.github.chhsiaoninety.nitmproxy.handler.protocol.http1.Http1BackendHandler;
import com.github.chhsiaoninety.nitmproxy.handler.protocol.http1.Http1FrontendHandler;
import com.github.chhsiaoninety.nitmproxy.handler.protocol.http2.Http2BackendHandler;
import com.github.chhsiaoninety.nitmproxy.handler.protocol.http2.Http2FrontendHandler;
import com.github.chhsiaoninety.nitmproxy.handler.protocol.tls.TlsBackendHandler;
import com.github.chhsiaoninety.nitmproxy.handler.protocol.tls.TlsFrontendHandler;

import io.netty.channel.ChannelHandler;

public class HandlerProvider {
    public ChannelHandler http1BackendHandler(
            NitmProxyMaster master, ConnectionContext connectionContext) {
        return new Http1BackendHandler(master, connectionContext);
    }

    public ChannelHandler http1FrontendHandler(
            NitmProxyMaster master, ConnectionContext connectionContext) {
        return new Http1FrontendHandler(master, connectionContext);
    }

    public ChannelHandler http2BackendHandler(
            NitmProxyMaster master, ConnectionContext connectionContext) {
        return new Http2BackendHandler(connectionContext);
    }

    public ChannelHandler http2FrontendHandler(
            NitmProxyMaster master, ConnectionContext connectionContext) {
        return new Http2FrontendHandler(connectionContext);
    }

    public ChannelHandler frontendTlsHandler(
            NitmProxyMaster master, ConnectionContext connectionContext) {
        return new TlsFrontendHandler(master, connectionContext);
    }

    public ChannelHandler backendTlsHandler(
            NitmProxyMaster master, ConnectionContext connectionContext) {
        return new TlsBackendHandler(master, connectionContext);
    }
}
