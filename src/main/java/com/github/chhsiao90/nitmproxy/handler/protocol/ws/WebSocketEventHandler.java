package com.github.chhsiao90.nitmproxy.handler.protocol.ws;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.listener.NitmProxyListener;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public class WebSocketEventHandler extends ChannelDuplexHandler {

    private NitmProxyListener listener;
    private ConnectionContext connectionContext;

    /**
     * Create new instance of web socket event handler.
     *
     * @param connectionContext the connection context
     */
    public WebSocketEventHandler(ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
        this.listener = connectionContext.listener();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
            throws Exception {
        if (msg instanceof WebSocketFrame) {
            listener.onReceivingWsFrame(connectionContext, (WebSocketFrame) msg);
        }
        ctx.write(msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof WebSocketFrame) {
            listener.onSendingWsFrame(connectionContext, (WebSocketFrame) msg);
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        listener.close(connectionContext);
    }
}
