package com.github.chhsiao90.nitmproxy.testing;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import org.assertj.core.api.AbstractObjectAssert;

import static java.nio.charset.StandardCharsets.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class RequestAssert extends AbstractObjectAssert<RequestAssert, FullHttpRequest> {

  public RequestAssert(FullHttpRequest actual) {
    super(actual, RequestAssert.class);
  }

  public RequestAssert hasMethod(HttpMethod method) {
    assertEquals(method, actual.method());
    return this;
  }

  public RequestAssert hasUrl(String uri) {
    assertEquals(uri, actual.uri());
    return this;
  }

  public RequestAssert hasHeader(String name, String value) {
    assertThat(actual.headers().get(name)).isEqualTo(value);
    return this;
  }

  public RequestAssert hasContent(String expect) {
    assertEquals(expect, actual.content().toString(UTF_8));
    return this;
  }

  public void release() {
    actual.release();
  }
}