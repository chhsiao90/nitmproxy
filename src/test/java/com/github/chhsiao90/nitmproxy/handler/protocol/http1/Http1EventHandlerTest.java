package com.github.chhsiao90.nitmproxy.handler.protocol.http1;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.listener.HttpListener;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpResponse;
import org.assertj.core.data.Offset;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static com.github.chhsiao90.nitmproxy.http.HttpUtil.*;
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
        when(listener.onHttp1Request(any(), any())).thenReturn(Optional.empty());

        assertTrue(channel.writeInbound(defaultRequest()));
        assertTrue(channel.writeOutbound(defaultResponse("Hello Nitmproxy")));

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
        when(listener.onHttp1Request(any(), any())).thenReturn(Optional.empty());

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        response.headers()
                .add(CONTENT_TYPE, "text/plain")
                .add(CONTENT_LENGTH, 15);
        assertTrue(channel.writeInbound(defaultRequest()));
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
        when(listener.onHttp1Request(any(), any())).thenReturn(Optional.of(defaultResponse("Hello Nitmproxy")));

        assertFalse(channel.writeInbound(defaultRequest()));
        assertChannel(channel)
                .hasOutboundMessage()
                .hasResponse()
                .isEqualTo(defaultResponse("Hello Nitmproxy"));

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
        assertThat(event.getRequestTime()).isCloseTo(currentTimeMillis(), Offset.offset(200L));
        assertEquals(OK, event.getStatus());
        assertEquals(TEXT_PLAIN.toString(), event.getContentType());
        assertThat(event.getResponseTime()).isGreaterThanOrEqualTo(0);
        assertEquals(15, event.getResponseBodySize());
    }

    @Test
    public void shouldPipeRequests() {
        assertTrue(channel.writeInbound(request(HTTP_1_1, GET, "localhost", "/first")));
        assertTrue(channel.writeInbound(request(HTTP_1_1, GET, "localhost", "/second")));
        assertTrue(channel.writeOutbound(defaultResponse("First Response")));
        assertTrue(channel.writeOutbound(defaultResponse("Second Response")));

        ArgumentCaptor<HttpEvent> captor = ArgumentCaptor.forClass(HttpEvent.class);
        verify(listener, times(2)).onHttpEvent(captor.capture());

        assertThat(captor.getAllValues()).hasSize(2);
        assertEquals("/first", captor.getAllValues().get(0).getPath());
        assertEquals("/second", captor.getAllValues().get(1).getPath());
    }

}
