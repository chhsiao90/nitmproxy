package com.github.chhsiao90.nitmproxy;

import com.github.chhsiao90.nitmproxy.exception.NitmProxyException;
import com.github.chhsiao90.nitmproxy.handler.ForwardBackendHandler;
import com.github.chhsiao90.nitmproxy.handler.ForwardEventHandler;
import com.github.chhsiao90.nitmproxy.handler.ForwardFrontendHandler;
import com.github.chhsiao90.nitmproxy.handler.ToClientHandler;
import com.github.chhsiao90.nitmproxy.handler.ToServerHandler;
import com.github.chhsiao90.nitmproxy.handler.protocol.ProtocolSelectHandler;
import com.github.chhsiao90.nitmproxy.handler.protocol.http1.Http1BackendHandler;
import com.github.chhsiao90.nitmproxy.handler.protocol.http1.Http1EventHandler;
import com.github.chhsiao90.nitmproxy.handler.protocol.http1.Http1FrontendHandler;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2BackendHandler;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2EventHandler;
import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FrontendHandler;
import com.github.chhsiao90.nitmproxy.handler.protocol.tls.TlsBackendHandler;
import com.github.chhsiao90.nitmproxy.handler.protocol.tls.TlsFrontendHandler;
import com.github.chhsiao90.nitmproxy.handler.protocol.ws.WebSocketBackendHandler;
import com.github.chhsiao90.nitmproxy.handler.protocol.ws.WebSocketFrontendHandler;
import io.netty.channel.ChannelHandler;

public class HandlerProvider {

    private NitmProxyMaster master;
    private ConnectionContext context;

    public HandlerProvider(NitmProxyMaster master, ConnectionContext context) {
        this.master = master;
        this.context = context;
    }

    public ChannelHandler protocolSelectHandler() {
        return new ProtocolSelectHandler(context);
    }

    public ChannelHandler frontendHandler(String protocol) {
        if (protocol.equals(Protocols.HTTP_1)) {
            return http1FrontendHandler();
        } else if (protocol.equals(Protocols.HTTP_2)) {
            return http2FrontendHandler();
        } else if (protocol.equals(Protocols.FORWARD)) {
            return forwardFrontendHandler();
        } else {
            throw new NitmProxyException("Unsupported protocol");
        }
    }

    public ChannelHandler backendHandler(String protocol) {
        if (protocol.equals(Protocols.HTTP_1)) {
            return http1BackendHandler();
        } else if (protocol.equals(Protocols.HTTP_2)) {
            return http2BackendHandler();
        } else if (protocol.equals(Protocols.FORWARD)) {
            return forwardBackendHandler();
        } else {
            throw new NitmProxyException("Unsupported protocol");
        }
    }

    public ChannelHandler http1BackendHandler() {
        return new Http1BackendHandler(context);
    }

    public ChannelHandler http1FrontendHandler() {
        return new Http1FrontendHandler(master, context);
    }

    public ChannelHandler wsBackendHandler() {
        return new WebSocketBackendHandler(context);
    }

    public ChannelHandler wsFrontendHandler() {
        return new WebSocketFrontendHandler(context);
    }

    public ChannelHandler http1EventHandler() {
        return new Http1EventHandler(master, context);
    }

    public ChannelHandler http2BackendHandler() {
        return new Http2BackendHandler(context);
    }

    public ChannelHandler http2FrontendHandler() {
        return new Http2FrontendHandler(context);
    }

    public ChannelHandler http2EventHandler() {
        return new Http2EventHandler(master, context);
    }

    public ChannelHandler tlsFrontendHandler() {
        return new TlsFrontendHandler(context);
    }

    public ChannelHandler tlsBackendHandler() {
        return new TlsBackendHandler(master, context);
    }

    public ChannelHandler toClientHandler() {
        return new ToClientHandler(context);
    }

    public ChannelHandler toServerHandler() {
        return new ToServerHandler(context);
    }

    public ChannelHandler forwardFrontendHandler() {
        return new ForwardFrontendHandler(context);
    }

    public ChannelHandler forwardBackendHandler() {
        return new ForwardBackendHandler(context);
    }

    public ChannelHandler forwardEventHandler() {
        return new ForwardEventHandler(master, context);
    }
}
