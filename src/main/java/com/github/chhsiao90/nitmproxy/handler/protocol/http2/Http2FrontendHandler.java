package com.github.chhsiao90.nitmproxy.handler.protocol.http2;

import static com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FrameWrapper.frameWrapper;
import static io.netty.handler.logging.LogLevel.DEBUG;

import com.github.chhsiao90.nitmproxy.ConnectionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
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
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;

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
        .addAfter(ctx.name(), null, new ToUpstreamHandler());
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (msg instanceof Http2FrameWrapper) {
      Http2FrameWrapper<?> frame = (Http2FrameWrapper<?>) msg;
      frame.write(ctx, http2ConnectionHandler.encoder(), frame.streamId(), promise);
      ctx.flush();
    } else {
      ctx.write(msg, promise);
    }
  }

  @Override
  public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding,
      boolean endOfStream) throws Http2Exception {
    ctx.fireChannelRead(frameWrapper(streamId,
        new DefaultHttp2DataFrame(data.retainedDuplicate(), endOfStream, padding)));
    return data.readableBytes() + padding;
  }

  @Override
  public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers,
      int padding, boolean endOfStream) throws Http2Exception {
    ctx.fireChannelRead(frameWrapper(streamId,
        new DefaultHttp2HeadersFrame(headers, endOfStream, padding)));
  }

  @Override
  public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers,
      int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream)
      throws Http2Exception {
    ctx.fireChannelRead(frameWrapper(streamId,
        new DefaultHttp2HeadersFrame(headers, endOfStream, padding)));
  }

  @Override
  public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency,
      short weight, boolean exclusive) throws Http2Exception {

  }

  @Override
  public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode)
      throws Http2Exception {
    ctx.writeAndFlush(frameWrapper(streamId,
        new DefaultHttp2ResetFrame(errorCode)));
  }

  @Override
  public void onSettingsAckRead(ChannelHandlerContext ctx) throws Http2Exception {
  }

  @Override
  public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings)
      throws Http2Exception {
    ctx.fireChannelRead(frameWrapper(0,
        new DefaultHttp2SettingsFrame(settings)));
  }

  @Override
  public void onPingRead(ChannelHandlerContext ctx, long data) throws Http2Exception {
  }

  @Override
  public void onPingAckRead(ChannelHandlerContext ctx, long data) throws Http2Exception {
  }

  @Override
  public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId,
      Http2Headers headers, int padding) throws Http2Exception {
  }

  @Override
  public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode,
      ByteBuf debugData) throws Http2Exception {
  }

  @Override
  public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement)
      throws Http2Exception {
    ctx.fireChannelRead(frameWrapper(streamId,
        new DefaultHttp2WindowUpdateFrame(windowSizeIncrement)));
  }

  @Override
  public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId,
      Http2Flags flags, ByteBuf payload) throws Http2Exception {
  }

  private class ToUpstreamHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      LOGGER.debug("{} : channelInactive", connectionContext);
      connectionContext.serverChannel().close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof Http2DataFrameWrapper) {
        Http2DataFrameWrapper frameWrapper = (Http2DataFrameWrapper) msg;
        connectionContext.serverChannel().writeAndFlush(frameWrapper);
      } else if (msg instanceof Http2FrameWrapper) {
        connectionContext.serverChannel().writeAndFlush(msg);
      } else {
        super.channelRead(ctx, msg);
      }
    }
  }
}