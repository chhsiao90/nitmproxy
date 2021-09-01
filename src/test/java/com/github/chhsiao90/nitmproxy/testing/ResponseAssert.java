package com.github.chhsiao90.nitmproxy.testing;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.InstanceOfAssertFactory;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class ResponseAssert extends AbstractObjectAssert<ResponseAssert, FullHttpResponse> {

  public ResponseAssert(FullHttpResponse actual) {
    super(actual, ResponseAssert.class);
  }

  public static InstanceOfAssertFactory<FullHttpResponse, ResponseAssert> asResponse() {
    return new InstanceOfAssertFactory<>(FullHttpResponse.class, ResponseAssert::new);
  }

  public ResponseAssert ok() {
    assertEquals(OK, actual.status());
    return this;
  }

  public ResponseAssert status(HttpResponseStatus expect) {
    assertEquals(expect, actual.status());
    return this;
  }

  public ByteBufAssert content() {
    return new ByteBufAssert(actual.content());
  }

  public ResponseAssert hasHeader(String name, String value) {
    assertThat(actual.headers().get(name)).isEqualTo(value);
    return this;
  }

  public void release() {
    actual.release();
  }
}
