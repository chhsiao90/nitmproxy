package com.github.chhsiaoninety.nitmproxy.handler.protocol.http2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2ResetFrame;
import io.netty.handler.codec.http2.Http2SettingsFrame;
import io.netty.handler.codec.http2.Http2WindowUpdateFrame;

public class Http2FrameWrapper<T extends Http2Frame> {
  protected int streamId;
  protected T frame;

  public Http2FrameWrapper(int streamId, T frame) {
    this.streamId = streamId;
    this.frame = frame;
  }

  public static Http2DataFrameWrapper frameWrapper(int streamId, DefaultHttp2DataFrame frame) {
    return new Http2DataFrameWrapper(streamId, frame);
  }

  public static <T extends Http2Frame> Http2FrameWrapper<T> frameWrapper(
      int streamId, T frame) {
    return new Http2FrameWrapper<T>(streamId, frame);
  }

  /**
   * Check if the msg is a wrapper of a frame.
   *
   * @param msg the msg
   * @param frameClass the frame class
   * @param <T> the Http2Frame class
   * @return {@code true} if msg is a wrapper for {@code frameClass}
   */
  public static <T extends Http2Frame> boolean isFrame(Object msg, Class<T> frameClass) {
    if (!(msg instanceof Http2FrameWrapper)) {
      return false;
    }
    Http2FrameWrapper<?> frameWrapper = (Http2FrameWrapper<?>) msg;
    return frameClass.isAssignableFrom(frameWrapper.frame.getClass());
  }

  public int streamId() {
    return streamId;
  }

  /**
   * Get the origin http2 frame from a wrapper.
   *
   * @param msg the msg
   * @return the origin http2 frame
   */
  public static Http2Frame frame(Object msg) {
    if (!(msg instanceof Http2FrameWrapper)) {
      throw new IllegalStateException("The msg must be a Http2FrameWrapper");
    }
    return ((Http2FrameWrapper<?>) msg).frame();
  }

  public T frame() {
    return frame;
  }

  @SuppressWarnings({"unchecked", "unused"})
  public <C extends Http2Frame> C frame(Class<C> frameType) {
    return (C) frame;
  }

  public boolean isHeaders() {
    return frame instanceof Http2HeadersFrame;
  }

  /**
   * Writes toe frame.
   *
   * @param ctx the ctx
   * @param encoder the encoder
   * @param streamId the stream id
   * @param promise the promise
   */
  public void write(ChannelHandlerContext ctx, Http2ConnectionEncoder encoder, int streamId,
      ChannelPromise promise) {
    if (frame instanceof Http2HeadersFrame) {
      Http2HeadersFrame headersFrame = (Http2HeadersFrame) frame;
      encoder.writeHeaders(ctx, streamId, headersFrame.headers(), headersFrame.padding(),
          headersFrame.isEndStream(), promise);
    } else if (frame instanceof Http2DataFrame) {
      Http2DataFrame dataFrame = (Http2DataFrame) frame;
      encoder.writeData(ctx, streamId, dataFrame.content(), dataFrame.padding(),
          dataFrame.isEndStream(), promise);
    } else if (frame instanceof Http2ResetFrame) {
      Http2ResetFrame resetFrame = (Http2ResetFrame) frame;
      encoder.writeRstStream(ctx, streamId, resetFrame.errorCode(), promise);
    } else if (frame instanceof Http2WindowUpdateFrame) {
      Http2WindowUpdateFrame windowUpdateFrame = (Http2WindowUpdateFrame) frame;
      encoder.writeWindowUpdate(ctx, streamId, windowUpdateFrame.windowSizeIncrement(), promise);
    } else if (frame instanceof Http2SettingsFrame) {
      Http2SettingsFrame settingsFrame = (Http2SettingsFrame) frame;
      encoder.writeSettings(ctx, settingsFrame.settings(), promise);
    }
  }
}
