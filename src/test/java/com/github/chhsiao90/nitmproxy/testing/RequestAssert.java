package com.github.chhsiao90.nitmproxy.testing;

import org.assertj.core.api.AbstractObjectAssert;

import io.netty.handler.codec.http.FullHttpRequest;

public class RequestAssert extends AbstractObjectAssert<RequestAssert, FullHttpRequest> {

  public RequestAssert(FullHttpRequest actual) {
    super(actual, RequestAssert.class);
  }

  public void release() {
    actual.release();
  }
}
