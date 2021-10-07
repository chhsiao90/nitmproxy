package com.github.chhsiao90.nitmproxy.handler.protocol.ws;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.listener.HttpListener;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public class WebSocketEventHandler extends ChannelDuplexHandler {

    private HttpListener listener;
    private ConnectionContext connectionContext;

    /**
     * Create new instance of web socket event handler.
     *
     * @param master            the master
     * @param connectionContext the connection context
     */
    public WebSocketEventHandler(
            NitmProxyMaster master,
            ConnectionContext connectionContext) {
        this.listener = master.httpEventListener();
        this.connectionContext = connectionContext;
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
}
