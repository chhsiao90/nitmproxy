package com.github.chhsiaoninety.nitmproxy.testing;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.StringAssert;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

public class ResponseAssert extends AbstractObjectAssert<ResponseAssert, FullHttpResponse> {

  public ResponseAssert(FullHttpResponse actual) {
    super(actual, ResponseAssert.class);
  }

  public ResponseAssert ok() {
    assertEquals(OK, actual.status());
    return this;
  }

  public ResponseAssert status(HttpResponseStatus expect) {
    assertEquals(expect, actual.status());
    return this;
  }

  public StringAssert text() {
    return new StringAssert(actual.content().toString(UTF_8));
  }

  public ResponseAssert hasHeader(String name, String value) {
    assertThat(actual.headers().get(name)).isEqualTo(value);
    return this;
  }

  public void release() {
    actual.release();
  }
}
