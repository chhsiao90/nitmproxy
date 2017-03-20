package com.github.chhsiaoninety.nitmproxy.handler.protocol.tls;

import com.github.chhsiaoninety.nitmproxy.ConnectionInfo;
import com.github.chhsiaoninety.nitmproxy.NitmProxyMaster;
import com.github.chhsiaoninety.nitmproxy.enums.Handler;
import com.github.chhsiaoninety.nitmproxy.tls.TlsUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TlsHandler extends ChannelOutboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TlsHandler.class);

    private NitmProxyMaster master;
    private ConnectionInfo connectionInfo;
    private Channel outboundChannel;
    private boolean client;

    private final List<Object> pendings;

    public TlsHandler(NitmProxyMaster master,
                      ConnectionInfo connectionInfo, Channel outboundChannel,
                      boolean client) {
        this.master = master;
        this.connectionInfo = connectionInfo;
        this.outboundChannel = outboundChannel;
        this.client = client;

        pendings = new ArrayList<>();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : handlerAdded", connectionInfo.toString(client));

        if (master.config().getHttpsPorts().contains(connectionInfo.getServerAddr().getPort())) {
            SslHandler sslHandler = sslCtx().newHandler(ctx.alloc());
            ctx.pipeline()
               .addBefore(ctx.name(), null, sslHandler)
               .addBefore(ctx.name(), null, new AlpnHandler(ctx));
        } else {
            configHttp1(ctx);
        }
    }

    private SslContext sslCtx() throws SSLException {
        if (client) {
            return TlsUtil.ctxForServer(master.config(), connectionInfo.getServerAddr().getHost());
        } else {
            return TlsUtil.ctxForClient(master.config());
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : handlerRemoved", connectionInfo.toString(client));

        flushPendings(ctx);
        ctx.flush();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        synchronized (pendings) {
            pendings.add(msg);
        }
        if (ctx.isRemoved()) {
            flushPendings(ctx);
            ctx.flush();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("{} : exceptionCaught, message is {}", connectionInfo.toString(client), cause.getMessage());

        outboundChannel.close();
        ctx.close();
    }

    private void flushPendings(ChannelHandlerContext ctx) {
        synchronized (pendings) {
            Iterator<Object> iterator = pendings.iterator();
            while (iterator.hasNext()) {
                ctx.write(iterator.next());
                iterator.remove();
            }
        }
    }

    private void configHttp1(ChannelHandlerContext ctx) {
        if (client) {
            ctx.pipeline().replace(this, null, master.handler(Handler.HTTP1_FRONTEND, connectionInfo, outboundChannel));
        } else {
            ctx.pipeline().replace(this, null, master.handler(Handler.HTTP1_BACKEND, connectionInfo, outboundChannel));
        }
    }

    private void configHttp2(ChannelHandlerContext ctx) {
        if (client) {
            ctx.pipeline().replace(this, null, master.handler(Handler.HTTP2_FRONTEND, connectionInfo, outboundChannel));
        } else {
            ctx.pipeline().replace(this, null, master.handler(Handler.HTTP2_BACKEND, connectionInfo, outboundChannel));
        }
    }

    private class AlpnHandler extends ApplicationProtocolNegotiationHandler {
        private ChannelHandlerContext tlsCtx;

        private AlpnHandler(ChannelHandlerContext tlsCtx) {
            super(ApplicationProtocolNames.HTTP_1_1);
            this.tlsCtx = tlsCtx;
        }

        @Override
        protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
            if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
                configHttp2(tlsCtx);
            } else if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
                configHttp1(tlsCtx);
            } else {
                throw new IllegalStateException("unknown protocol: " + protocol);
            }
        }
    }
}
