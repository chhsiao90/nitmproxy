package com.github.chhsiao90.nitmproxy.handler.protocol.http2;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.listener.NitmProxyListener;
import com.google.common.collect.ImmutableList;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import org.assertj.core.data.Offset;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static com.github.chhsiao90.nitmproxy.http.HttpUtil.*;
import static com.github.chhsiao90.nitmproxy.listener.NitmProxyListenerProvider.*;
import static com.github.chhsiao90.nitmproxy.testing.EmbeddedChannelAssert.*;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static java.lang.System.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class Http2EventHandlerTest {

    private NitmProxyListener listener;
    private EmbeddedChannel channel;

    @Before
    public void setUp() {
        listener = mock(NitmProxyListener.class);
        NitmProxyMaster master = mock(NitmProxyMaster.class);
        when(master.createListener()).thenReturn(listener);

        ConnectionContext context = new ConnectionContext(master)
                .withClientAddr(new Address("localhost", 8080))
                .withClientChannel(channel);
        Http2EventHandler handler = new Http2EventHandler(master, context);
        channel = new EmbeddedChannel(handler);
    }

    @After
    public void tearDown() {
        channel.finishAndReleaseAll();
    }

    @Test
    public void shouldSendRequestAfterRequestEnded() {
        when(listener.onHttp2Request(any(), any())).thenReturn(Optional.empty());
        List<Http2FrameWrapper<?>> requestFrames = Http2FramesWrapper
                .builder(1)
                .request(textRequest(HttpVersion.HTTP_1_1, POST, "localhost", "/", "Hello nitmproxy"))
                .build()
                .getAllFrames();
        assertFalse(channel.writeInbound(requestFrames.get(0)));
        assertTrue(channel.writeInbound(requestFrames.get(1)));
        assertChannel(channel)
                .hasInboundMessage()
                .hasSize(2);
        assertThat(channel.inboundMessages().poll()).isEqualTo(requestFrames.get(0));
        assertThat(channel.inboundMessages().poll()).isEqualTo(requestFrames.get(1));
        requestFrames.forEach(ReferenceCountUtil::release);
    }

    @Test
    public void shouldLogWithFullResponse() {
        when(listener.onHttp2Request(any(), any())).thenReturn(Optional.empty());
        when(listener.onHttp2Response(any(), any())).thenAnswer(invocation -> {
            Http2FrameWrapper<?> frame = (Http2FrameWrapper<?>) invocation.getArguments()[1];
            return ImmutableList.of(frame);
        });

        Http2FramesWrapper
                .builder(1)
                .request(defaultRequest())
                .build()
                .getAllFrames()
                .forEach(channel::writeInbound);
        Http2FramesWrapper
                .builder(1)
                .response(defaultResponse("Hello nitmproxy"))
                .build()
                .getAllFrames()
                .forEach(channel::writeOutbound);

        ArgumentCaptor<HttpEvent> captor = ArgumentCaptor.forClass(HttpEvent.class);
        verify(listener).onHttpEvent(captor.capture());
        HttpEvent event = captor.getValue();
        assertEquals(new Address("localhost", 8080), event.getClient());
        assertThat(event.getServer()).isNull();
        assertEquals(GET, event.getMethod());
        assertEquals(HTTP_2, event.getVersion());
        assertEquals("localhost", event.getHost());
        assertEquals("/", event.getPath());
        assertEquals(0, event.getRequestBodySize());
        assertThat(event.getRequestTime()).isCloseTo(currentTimeMillis(), Offset.offset(100L));
        assertEquals(OK, event.getStatus());
        assertEquals(TEXT_PLAIN.toString(), event.getContentType());
        assertThat(event.getResponseTime()).isGreaterThanOrEqualTo(0);
        assertEquals(15, event.getResponseBodySize());
    }

    @Test
    public void shouldInterceptWithResponse() {
        when(listener.onHttp2Request(any(), any())).thenReturn(Optional.of(Http2FramesWrapper
                .builder(1)
                .response(defaultResponse("Hello nitmproxy"))
                .build()));
        Http2FramesWrapper
                .builder(1)
                .request(defaultRequest())
                .build()
                .getAllFrames()
                .forEach(channel::writeInbound);

        assertChannel(channel).hasOutboundMessage().hasSize(2);

        ArgumentCaptor<HttpEvent> captor = ArgumentCaptor.forClass(HttpEvent.class);
        verify(listener).onHttpEvent(captor.capture());
        HttpEvent event = captor.getValue();
        assertEquals(new Address("localhost", 8080), event.getClient());
        assertThat(event.getServer()).isNull();
        assertEquals(GET, event.getMethod());
        assertEquals(HTTP_2, event.getVersion());
        assertEquals("localhost", event.getHost());
        assertEquals("/", event.getPath());
        assertEquals(0, event.getRequestBodySize());
        assertThat(event.getRequestTime()).isCloseTo(currentTimeMillis(), Offset.offset(100L));
        assertEquals(OK, event.getStatus());
        assertEquals(TEXT_PLAIN.toString(), event.getContentType());
        assertThat(event.getResponseTime()).isGreaterThanOrEqualTo(0);
        assertEquals(15, event.getResponseBodySize());
    }
}
