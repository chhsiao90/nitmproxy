package com.github.chhsiao90.nitmproxy.handler.protocol.http1;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.listener.HttpListener;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpResponse;
import org.assertj.core.data.Offset;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static com.github.chhsiao90.nitmproxy.HttpObjectUtil.*;
import static com.github.chhsiao90.nitmproxy.testing.EmbeddedChannelAssert.*;
import static com.google.common.net.HttpHeaders.*;
import static io.netty.buffer.Unpooled.*;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;
import static java.lang.System.*;
import static java.nio.charset.StandardCharsets.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class Http1EventHandlerTest {
    private HttpListener listener;
    private EmbeddedChannel channel;

    @Before
    public void setUp() {
        listener = mock(HttpListener.class);
        NitmProxyMaster master = mock(NitmProxyMaster.class);
        when(master.httpEventListener()).thenReturn(listener);

        ConnectionContext context = new ConnectionContext(master)
                .withClientAddr(new Address("localhost", 8080))
                .withClientChannel(channel);
        Http1EventHandler handler = new Http1EventHandler(master, context);
        channel = new EmbeddedChannel(handler);
    }

    @After
    public void tearDown() {
        channel.finishAndReleaseAll();
    }

    @Test
    public void shouldLogWithFullResponse() {
        when(listener.onHttp1Request(any())).thenReturn(Optional.empty());

        assertTrue(channel.writeInbound(fullRequest()));
        assertTrue(channel.writeOutbound(fullResponse("Hello Nitmproxy")));

        ArgumentCaptor<HttpEvent> captor = ArgumentCaptor.forClass(HttpEvent.class);
        verify(listener).onHttpEvent(captor.capture());
        HttpEvent event = captor.getValue();
        assertEquals(new Address("localhost", 8080), event.getClient());
        assertThat(event.getServer()).isNull();
        assertEquals(GET, event.getMethod());
        assertEquals(HTTP_1_1, event.getVersion());
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
    public void shouldLogWithResponseAndContent() {
        when(listener.onHttp1Request(any())).thenReturn(Optional.empty());

        HttpResponse response = response();
        response.headers().add(CONTENT_LENGTH, 15);
        assertTrue(channel.writeInbound(fullRequest()));
        assertTrue(channel.writeOutbound(response,
                new DefaultHttpContent(copiedBuffer("Hello ".getBytes(UTF_8))),
                new DefaultLastHttpContent(copiedBuffer("Nitmproxy".getBytes(UTF_8)))));

        ArgumentCaptor<HttpEvent> captor = ArgumentCaptor.forClass(HttpEvent.class);
        verify(listener).onHttpEvent(captor.capture());
        HttpEvent event = captor.getValue();
        assertEquals(new Address("localhost", 8080), event.getClient());
        assertThat(event.getServer()).isNull();
        assertEquals(GET, event.getMethod());
        assertEquals(HTTP_1_1, event.getVersion());
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
        when(listener.onHttp1Request(any())).thenReturn(Optional.of(fullResponse("Hello Nitmproxy")));

        assertFalse(channel.writeInbound(fullRequest()));
        assertChannel(channel)
                .hasOutboundMessage()
                .hasResponse()
                .isEqualTo(fullResponse("Hello Nitmproxy"));

        ArgumentCaptor<HttpEvent> captor = ArgumentCaptor.forClass(HttpEvent.class);
        verify(listener).onHttpEvent(captor.capture());
        HttpEvent event = captor.getValue();
        assertEquals(new Address("localhost", 8080), event.getClient());
        assertThat(event.getServer()).isNull();
        assertEquals(GET, event.getMethod());
        assertEquals(HTTP_1_1, event.getVersion());
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