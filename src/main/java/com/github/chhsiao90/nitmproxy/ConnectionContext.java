package com.github.chhsiao90.nitmproxy;

import com.github.chhsiao90.nitmproxy.handler.proxy.HttpProxyHandler;
import com.github.chhsiao90.nitmproxy.handler.proxy.SocksProxyHandler;
import com.github.chhsiao90.nitmproxy.handler.proxy.TransparentProxyHandler;
import com.github.chhsiao90.nitmproxy.listener.NitmProxyListener;
import com.github.chhsiao90.nitmproxy.tls.TlsContext;
import com.github.chhsiao90.nitmproxy.ws.WebSocketContext;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;

import static java.lang.String.*;

public class ConnectionContext {

    private NitmProxyMaster master;
    private HandlerProvider provider;
    private NitmProxyListener listener;

    private ByteBufAllocator alloc;

    private Address clientAddr;
    private Address serverAddr;

    private Channel clientChannel;
    private Channel serverChannel;

    private TlsContext tlsCtx;
    private WebSocketContext wsCtx;

    public ConnectionContext(NitmProxyMaster master) {
        this.master = master;
        this.provider = master.provider(this);
        this.tlsCtx = new TlsContext();
        this.wsCtx = new WebSocketContext();
        this.listener = master.listenerProvider().create();
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

    public ConnectionContext withClientChannel(Channel clientChannel) {
        this.clientChannel = clientChannel;
        return this;
    }

    public ConnectionContext withServerChannel(Channel serverChannel) {
        this.serverChannel = serverChannel;
        return this;
    }

    public ConnectionContext withAlloc(ByteBufAllocator alloc) {
        this.alloc = alloc;
        return this;
    }

    public ByteBufAllocator alloc() {
        return alloc;
    }

    public NitmProxyMaster master() {
        return master;
    }

    public NitmProxyConfig config() {
        return master.config();
    }

    public ChannelHandler proxyHandler() {
        switch (master.config().getProxyMode()) {
            case HTTP:
                return new HttpProxyHandler(this);
            case SOCKS:
                return new SocksProxyHandler(master, this);
            case TRANSPARENT:
                return new TransparentProxyHandler(this);
            default:
                throw new IllegalStateException("No proxy mode available: " + master.config().getProxyMode());
        }
    }

    public HandlerProvider provider() {
        return provider;
    }

    public boolean connected() {
        return serverChannel != null;
    }

    public ChannelFuture connect(Address address, ChannelHandlerContext fromCtx) {
        if (serverChannel != null && (!serverAddr.equals(address) || !serverChannel.isActive())) {
            serverChannel.close();
            serverChannel = null;
        }
        if (serverChannel != null) {
            return serverChannel.newSucceededFuture();
        }

        tlsCtx.protocols(fromCtx.executor().newPromise());
        tlsCtx.protocol(fromCtx.executor().newPromise());
        serverAddr = address;
        return master.connect(fromCtx, this, new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                withServerChannel(ch);
                ch.pipeline().addLast(provider().tlsBackendHandler());
                ch.pipeline().addLast(provider().tailBackendHandler());
                listener().onConnect(ConnectionContext.this, ch);
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

    public WebSocketContext wsCtx() {
        return wsCtx;
    }

    public NitmProxyListener listener() {
        return listener;
    }

    public void close() {
        listener.close(this);
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
