package com.github.chhsiao90.nitmproxy.testing;

import io.netty.handler.codec.http2.Http2DataFrame;
import org.assertj.core.api.AbstractObjectAssert;

import static org.junit.Assert.*;

public class Http2DataFrameAssert
    extends AbstractObjectAssert<Http2DataFrameAssert, Http2DataFrame> {

  public Http2DataFrameAssert(Http2DataFrame actual) {
    super(actual, Http2DataFrameAssert.class);
  }

  public Http2DataFrameAssert hasEndStream(boolean expect) {
    assertEquals(expect, actual.isEndStream());
    return this;
  }

  public ByteBufAssert hasContent() {
    return new ByteBufAssert(actual.content());
  }

  public void release() {
    actual.release();
  }
}
