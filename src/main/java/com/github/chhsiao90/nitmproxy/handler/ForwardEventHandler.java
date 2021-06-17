package com.github.chhsiao90.nitmproxy.handler;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.event.ForwardEvent;
import com.github.chhsiao90.nitmproxy.listener.ForwardListener;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import static java.lang.System.currentTimeMillis;

public class ForwardEventHandler extends ChannelDuplexHandler {

    private ForwardListener listener;
    private ConnectionContext connectionContext;

    private long requestTime;
    private long requestBytes;

    /**
     * Create new instance of http1 event handler.
     *
     * @param master            the master
     * @param connectionContext the connection context
     */
    public ForwardEventHandler(
            NitmProxyMaster master,
            ConnectionContext connectionContext) {
        this.listener = master.forwardEventListener();
        this.connectionContext = connectionContext;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
            throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        listener.onForwardResponse(connectionContext, byteBuf);
        long responseTime = currentTimeMillis();
        ForwardEvent forwardEvent = ForwardEvent.builder(connectionContext)
                .requestBodySize(requestBytes)
                .requestTime(requestTime)
                .responseTime(responseTime)
                .responseBodySize(byteBuf.readableBytes())
                .build();
        try {
            listener.onForwardEvent(connectionContext, forwardEvent);
        } finally {
            requestTime = 0;
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        listener.onForwardRequest(connectionContext, byteBuf);
        requestBytes = byteBuf.readableBytes();
        requestTime = currentTimeMillis();
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
    }
}
