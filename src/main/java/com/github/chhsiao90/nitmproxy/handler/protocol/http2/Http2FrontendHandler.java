package com.github.chhsiao90.nitmproxy.handler.protocol.http2;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.DefaultHttp2SettingsFrame;
import io.netty.handler.codec.http2.DefaultHttp2WindowUpdateFrame;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FrameWrapper.*;
import static com.github.chhsiao90.nitmproxy.util.LogWrappers.*;
import static io.netty.handler.logging.LogLevel.*;
import static io.netty.util.ReferenceCountUtil.*;

public class Http2FrontendHandler
        extends ChannelOutboundHandlerAdapter
        implements Http2FrameListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Http2FrontendHandler.class);

    private ConnectionContext connectionContext;
    private Http2ConnectionHandler http2ConnectionHandler;

    public Http2FrontendHandler(ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("{} : handlerAdded", connectionContext);

        Http2Connection http2Connection = new DefaultHttp2Connection(true);
        http2ConnectionHandler = new Http2ConnectionHandlerBuilder()
                .connection(http2Connection)
                .frameListener(this)
                .frameLogger(new Http2FrameLogger(DEBUG))
                .build();

        ctx.pipeline()
            .addBefore(ctx.name(), null, http2ConnectionHandler)
            .addAfter(ctx.name(), null, connectionContext.provider().http2EventHandler());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        LOGGER.debug("{} : handlerRemoved", connectionContext);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
            throws Exception {
        if (msg instanceof Http2FrameWrapper) {
            Http2FrameWrapper<?> frame = (Http2FrameWrapper<?>) touch(msg,
                    format("%s context=%s", msg, connectionContext));
            frame.write(ctx, http2ConnectionHandler.encoder(), frame.streamId(), promise);
        } else {
            ctx.write(msg, promise);
        }
    }

    @Override
    public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding,
                          boolean endOfStream) {
        int processed = data.readableBytes() + padding;
        ctx.fireChannelRead(frameWrapper(streamId, new DefaultHttp2DataFrame(data.retain(), endOfStream, padding)));
        return processed;
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers,
                              int padding, boolean endOfStream) {
        ctx.fireChannelRead(frameWrapper(streamId, new DefaultHttp2HeadersFrame(headers, endOfStream, padding)));
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers,
                              int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream) {
        ctx.fireChannelRead(frameWrapper(streamId, new DefaultHttp2HeadersFrame(headers, endOfStream, padding)));
    }

    @Override
    public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency,
                               short weight, boolean exclusive) {
    }

    @Override
    public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) {
        ctx.writeAndFlush(frameWrapper(streamId, new DefaultHttp2ResetFrame(errorCode)));
    }

    @Override
    public void onSettingsAckRead(ChannelHandlerContext ctx) {
    }

    @Override
    public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) {
        ctx.fireChannelRead(frameWrapper(0, new DefaultHttp2SettingsFrame(settings)));
    }

    @Override
    public void onPingRead(ChannelHandlerContext ctx, long data) {
    }

    @Override
    public void onPingAckRead(ChannelHandlerContext ctx, long data) {
    }

    @Override
    public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId,
                                  Http2Headers headers, int padding) {
    }

    @Override
    public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode,
                             ByteBuf debugData) {
    }

    @Override
    public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) {
        ctx.fireChannelRead(frameWrapper(streamId,
                                         new DefaultHttp2WindowUpdateFrame(windowSizeIncrement)));
    }

    @Override
    public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId,
                               Http2Flags flags, ByteBuf payload) {
    }
}