package com.github.chhsiaoninety.nitmproxy;

import com.github.chhsiaoninety.nitmproxy.handler.protocol.http1.Http1BackendHandler;
import com.github.chhsiaoninety.nitmproxy.handler.protocol.http1.Http1FrontendHandler;
import com.github.chhsiaoninety.nitmproxy.handler.protocol.http2.Http2BackendHandler;
import com.github.chhsiaoninety.nitmproxy.handler.protocol.http2.Http2FrontendHandler;
import com.github.chhsiaoninety.nitmproxy.handler.protocol.tls.TlsHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;

public class HandlerProvider {
    public ChannelHandler http1BackendHandler(
            NitmProxyMaster master, ConnectionInfo connectionInfo, Channel outboundChannel) {
        return new Http1BackendHandler(master, connectionInfo, outboundChannel);
    }

    public ChannelHandler http1FrontendHandler(
            NitmProxyMaster master, ConnectionInfo connectionInfo) {
        return new Http1FrontendHandler(master, connectionInfo);
    }

    public ChannelHandler http1FrontendHandler(
            NitmProxyMaster master, ConnectionInfo connectionInfo, Channel outboundChannel) {
        return new Http1FrontendHandler(master, connectionInfo, outboundChannel);
    }

    public ChannelHandler http2BackendHandler(
            NitmProxyMaster master, ConnectionInfo connectionInfo, Channel outboundChannel) {
        return new Http2BackendHandler(master, connectionInfo, outboundChannel);
    }

    public ChannelHandler http2FrontendHandler(
            NitmProxyMaster master, ConnectionInfo connectionInfo, Channel outboundChannel) {
        return new Http2FrontendHandler(master, connectionInfo, outboundChannel);
    }

    public ChannelHandler frontendTlsHandler(
            NitmProxyMaster master, ConnectionInfo connectionInfo, Channel outboundChannel) {
        return new TlsHandler(master, connectionInfo, outboundChannel, true);
    }

    public ChannelHandler backendTlsHandler(
            NitmProxyMaster master, ConnectionInfo connectionInfo, Channel outboundChannel) {
        return new TlsHandler(master, connectionInfo, outboundChannel, false);
    }
}
