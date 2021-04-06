package com.github.chhsiaoninety.nitmproxy;

import static java.lang.String.format;

import com.github.chhsiaoninety.nitmproxy.enums.Handler;
import com.github.chhsiaoninety.nitmproxy.handler.proxy.HttpProxyHandler;
import com.github.chhsiaoninety.nitmproxy.handler.proxy.SocksProxyHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;

public class ConnectionContext
{
    private NitmProxyMaster master;

    private Address clientAddr;
    private Address serverAddr;

    private Channel clientChannel;
    private Channel serverChannel;

    private TlsContext tlsCtx;

    public ConnectionContext(NitmProxyMaster master) {
        this.master = master;
        this.tlsCtx = new TlsContext();
    }

    public ConnectionContext withClientAddr(Address clientAddr) {
        this.clientAddr = clientAddr;
        return this;
    }

    public Address getClientAddr() {
        return clientAddr;
    }

    public ConnectionContext withServerAddr(Address serverAddr) {
        this.serverAddr = serverAddr;
        return this;
    }

    public Address getServerAddr() {
        return serverAddr;
    }

    public ConnectionContext withClientChannel(Channel clientChannel)
    {
        this.clientChannel = clientChannel;
        return this;
    }

    public ConnectionContext withServerChannel(Channel serverChannel)
    {
        this.serverChannel = serverChannel;
        return this;
    }

    public NitmProxyConfig config() {
        return master.config();
    }

    public ChannelHandler proxyHandler() {
        switch (master.config().getProxyMode()) {
            case HTTP:
                return new HttpProxyHandler(master, this);
            case SOCKS:
                return new SocksProxyHandler(master, this);
            default:
                throw new IllegalStateException("No proxy mode available: " + master.config().getProxyMode());
        }
    }

    public ChannelHandler handler(Handler handler) {
        HandlerProvider handlerProvider = master.provider();
        switch (handler) {
            case HTTP1_BACKEND:
                return handlerProvider.http1BackendHandler(master, this);
            case HTTP1_FRONTEND:
                return handlerProvider.http1FrontendHandler(master, this);
            case HTTP2_BACKEND:
                return handlerProvider.http2BackendHandler(master, this);
            case HTTP2_FRONTEND:
                return handlerProvider.http2FrontendHandler(master, this);
            case TLS_BACKEND:
                return handlerProvider.backendTlsHandler(master, this);
            case TLS_FRONTEND:
                return handlerProvider.frontendTlsHandler(master, this);
            default:
                throw new IllegalStateException("No handler found with: " + handler);
        }
    }

    public boolean isHttps() {
        return master.config().getHttpsPorts().contains(serverAddr.getPort());
    }

    public boolean connected() {
        return serverChannel != null;
    }

    public ChannelFuture connect(Address address, ChannelHandlerContext fromCtx) {
        if (serverChannel == null) {
            tlsCtx.protocols(fromCtx.executor().newPromise());
            tlsCtx.protocol(fromCtx.executor().newPromise());
        }
        if (serverChannel != null && (!serverAddr.equals(address) || !serverChannel.isActive())) {
            serverChannel.close();
            serverChannel = null;
        }
        if (serverChannel != null) {
            return serverChannel.newSucceededFuture();
        }

        serverAddr = address;
        return master.connect(fromCtx, this, new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(withServerChannel(ch).handler(Handler.TLS_BACKEND));
            }
        });
    }

    public Channel serverChannel() {
        return serverChannel;
    }

    public Channel clientChannel() {
        return clientChannel;
    }

    public TlsContext tlsCtx() {
        return tlsCtx;
    }

    @Override
    public String toString() {
        if (serverAddr != null) {
            return format("[Client (%s)] <=> [Server (%s)]",
                clientAddr, serverAddr);
        }
        return format("[Client (%s)] <=> [PROXY]", clientAddr);
    }

    public String toString(boolean client) {
        if (client) {
            return format("[Client (%s)] <=> [PROXY]", clientAddr);
        } else {
            return format("[PROXY] <=> [Server (%s)]", serverAddr);
        }
    }
}
