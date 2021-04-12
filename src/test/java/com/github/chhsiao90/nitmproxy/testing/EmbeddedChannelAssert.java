package com.github.chhsiao90.nitmproxy.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import io.netty.channel.embedded.EmbeddedChannel;

public class EmbeddedChannelAssert extends AbstractAssert<EmbeddedChannelAssert, EmbeddedChannel> {
  public EmbeddedChannelAssert(EmbeddedChannel actual) {
    super(actual, EmbeddedChannelAssert.class);
  }

  public ChannelMessagesAssert hasOutboundMessage() {
    assertThat(actual.outboundMessages()).isNotEmpty();
    return new ChannelMessagesAssert(actual.outboundMessages());
  }

  public ChannelMessagesAssert outbound() {
    return new ChannelMessagesAssert(actual.outboundMessages());
  }

  public ChannelMessagesAssert hasInboundMessage() {
    assertThat(actual.inboundMessages()).isNotEmpty();
    return new ChannelMessagesAssert(actual.inboundMessages());
  }

  public ChannelMessagesAssert inbound() {
    return new ChannelMessagesAssert(actual.inboundMessages());
  }

  public static EmbeddedChannelAssert assertChannel(EmbeddedChannel actual) {
    return new EmbeddedChannelAssert(actual);
  }
}
