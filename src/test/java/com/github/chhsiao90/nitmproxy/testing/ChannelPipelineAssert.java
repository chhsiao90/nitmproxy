package com.github.chhsiao90.nitmproxy.testing;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import org.assertj.core.api.AbstractAssert;

import static org.assertj.core.api.Assertions.*;

public class ChannelPipelineAssert extends AbstractAssert<ChannelPipelineAssert, ChannelPipeline> {
    protected ChannelPipelineAssert(ChannelPipeline actual) {
        super(actual, ChannelPipelineAssert.class);
    }

    public ChannelPipelineAssert hasHandlers(Class<? extends ChannelHandler>... handlers) {
        Class<?>[] actual = this.actual
                .toMap()
                .values()
                .stream()
                .map(ChannelHandler::getClass)
                .toArray(Class[]::new);
        assertThat(actual).containsExactly(handlers);
        return this;
    }
}
