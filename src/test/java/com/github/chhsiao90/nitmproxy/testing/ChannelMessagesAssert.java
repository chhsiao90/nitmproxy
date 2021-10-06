package com.github.chhsiao90.nitmproxy.testing;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.util.Streams;

import java.util.ArrayDeque;
import java.util.Queue;

import static io.netty.util.ReferenceCountUtil.*;
import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.*;

public class ChannelMessagesAssert
    extends AbstractIterableAssert<ChannelMessagesAssert, Queue<Object>, Object, ObjectAssert<Object>> {
  public ChannelMessagesAssert(Queue<Object> actual) {
    super(actual, ChannelMessagesAssert.class);
  }

  @Override
  protected ObjectAssert<Object> toAssert(Object value, String description) {
    return new ObjectAssert<>(value);
  }

  @Override
  protected ChannelMessagesAssert newAbstractIterableAssert(Iterable<?> iterable) {
    return new ChannelMessagesAssert(
        Streams.stream(iterable).collect(toCollection(ArrayDeque::new)));
  }

  public ChannelMessagesAssert has(Class<?> type) {
    assertThat(actual).isNotEmpty();
    assertThat(actual.peek()).isInstanceOf(type);
    return this;
  }

  public ByteBufAssert hasByteBuf() {
    has(ByteBuf.class);
    return new ByteBufAssert((ByteBuf) actual.poll());
  }

  public ResponseAssert hasResponse() {
    FullHttpResponse fullResponse = null;
    while (!actual.isEmpty() && actual.peek() instanceof HttpObject) {
      HttpObject httpObject = (HttpObject) actual.poll();
      if (httpObject instanceof HttpResponse) {
        HttpResponse response = (HttpResponse) httpObject;
        fullResponse = new DefaultFullHttpResponse(response.protocolVersion(),
            response.status(), Unpooled.buffer(), response.headers(), new DefaultHttpHeaders());
      }
      if (httpObject instanceof HttpContent) {
        assertThat(fullResponse).isNotNull();
        fullResponse.content().writeBytes(((HttpContent) httpObject).content());
        release(httpObject);
      }
      if (httpObject instanceof LastHttpContent) {
        break;
      }
    }
    return new ResponseAssert(fullResponse).isNotNull();
  }

  public RequestAssert hasRequest() {
    assertThat(actual).isNotEmpty();
    assertThat(actual.peek()).isInstanceOf(FullHttpRequest.class);
    return new RequestAssert((FullHttpRequest) actual.poll());
  }

  public ObjectAssert<Object> peek() {
    return new ObjectAssert<>(actual.peek());
  }
}
