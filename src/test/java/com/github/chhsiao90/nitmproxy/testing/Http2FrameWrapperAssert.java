package com.github.chhsiao90.nitmproxy.testing;

import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FrameWrapper;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.util.ReferenceCountUtil;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.InstanceOfAssertFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

@SuppressWarnings("rawtypes")
public class Http2FrameWrapperAssert extends AbstractObjectAssert<Http2FrameWrapperAssert, Http2FrameWrapper> {

  public static final InstanceOfAssertFactory<Http2FrameWrapper, Http2FrameWrapperAssert> HTTP2_FRAME = new InstanceOfAssertFactory<>(
      Http2FrameWrapper.class, Http2FrameWrapperAssert::new);

  public Http2FrameWrapperAssert(Http2FrameWrapper<?> actual) {
    super(actual, Http2FrameWrapperAssert.class);
  }

  public Http2FrameWrapperAssert hasStreamId(int streamId) {
    assertEquals(actual.streamId(), streamId);
    return this;
  }

  public Http2HeadersFrameAssert isHeadersFrame() {
    is(Http2HeadersFrame.class);
    return new Http2HeadersFrameAssert((Http2HeadersFrame) actual.frame());
  }

  public Http2DataFrameAssert isDataFrame() {
    is(Http2DataFrame.class);
    return new Http2DataFrameAssert((Http2DataFrame) actual.frame());
  }

  public Http2FrameWrapperAssert is(Class<? extends Http2Frame> frameClass) {
    assertThat(actual.frame()).isInstanceOf(frameClass);
    return this;
  }

  public void release() {
    ReferenceCountUtil.release(actual);
  }
}
