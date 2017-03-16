package com.github.chhsiaoninety.nitmproxy;

import com.github.chhsiaoninety.nitmproxy.layer.protocol.http1.Http1BackendHandler;
import com.github.chhsiaoninety.nitmproxy.layer.protocol.http1.Http1FrontendHandler;
import com.github.chhsiaoninety.nitmproxy.layer.protocol.http2.Http2BackendHandler;
import com.github.chhsiaoninety.nitmproxy.layer.protocol.http2.Http2FrontendHandler;
import com.github.chhsiaoninety.nitmproxy.layer.protocol.tls.TlsHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;

public class HandlerProvider {
    private NitmProxyConfig config;

    public HandlerProvider(NitmProxyConfig config) {
        this.config = config;
    }

    public ChannelHandler http1BackendHandler(
            ConnectionInfo connectionInfo, Channel outboundChannel) {
        return new Http1BackendHandler(config, connectionInfo, outboundChannel);
    }

    public ChannelHandler http1FrontendHandler(
            ConnectionInfo connectionInfo) {
        return new Http1FrontendHandler(this, config, connectionInfo);
    }

    public ChannelHandler http1FrontendHandler(
            ConnectionInfo connectionInfo, Channel outboundChannel) {
        return new Http1FrontendHandler(this, config, connectionInfo, outboundChannel);
    }

    public ChannelHandler http2BackendHandler(
            ConnectionInfo connectionInfo, Channel outboundChannel) {
        return new Http2BackendHandler(config, connectionInfo, outboundChannel);
    }

    public ChannelHandler http2FrontendHandler(
            ConnectionInfo connectionInfo, Channel outboundChannel) {
        return new Http2FrontendHandler(config, connectionInfo, outboundChannel);
    }

    public ChannelHandler frontendTlsHandler(ConnectionInfo connectionInfo, Channel outboundChannel) {
        return new TlsHandler(this, config, connectionInfo, outboundChannel, true);
    }

    public ChannelHandler backendTlsHandler(ConnectionInfo connectionInfo, Channel outboundChannel) {
        return new TlsHandler(this, config, connectionInfo, outboundChannel, false);
    }
}
