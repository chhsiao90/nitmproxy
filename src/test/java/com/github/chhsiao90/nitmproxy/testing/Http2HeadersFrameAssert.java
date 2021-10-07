package com.github.chhsiao90.nitmproxy.testing;

import io.netty.handler.codec.http2.Http2HeadersFrame;
import org.assertj.core.api.AbstractObjectAssert;

import static org.junit.Assert.*;

public class Http2HeadersFrameAssert
    extends AbstractObjectAssert<Http2HeadersFrameAssert, Http2HeadersFrame> {

  public Http2HeadersFrameAssert(Http2HeadersFrame actual) {
    super(actual, Http2HeadersFrameAssert.class);
  }

  public Http2HeadersFrameAssert hasEndStream(boolean expect) {
    assertEquals(expect, actual.isEndStream());
    return this;
  }

  public Http2HeadersFrameAssert hasHeader(String name, String value) {
    assertEquals(value, actual.headers().get(name));
    return this;
  }

  public Http2HeadersFrameAssert hasAuthority(String expect) {
    assertEquals(expect, actual.headers().authority().toString());
    return this;
  }

  public Http2HeadersFrameAssert hasMethod(String expect) {
    assertEquals(expect, actual.headers().method().toString());
    return this;
  }

  public Http2HeadersFrameAssert hasPath(String expect) {
    assertEquals(expect, actual.headers().path().toString());
    return this;
  }

  public Http2HeadersFrameAssert hasScheme(String expect) {
    assertEquals(expect, actual.headers().scheme().toString());
    return this;
  }

  public Http2HeadersFrameAssert hasStatus(String status) {
    assertEquals(status, actual.headers().status().toString());
    return this;
  }
}
