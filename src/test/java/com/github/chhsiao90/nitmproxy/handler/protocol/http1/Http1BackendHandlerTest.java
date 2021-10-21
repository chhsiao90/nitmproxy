package com.github.chhsiao90.nitmproxy.handler.protocol.http1;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.HandlerProvider;
import com.github.chhsiao90.nitmproxy.NitmProxyConfig;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.listener.NitmProxyListenerProvider;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.github.chhsiao90.nitmproxy.http.HttpUtil.*;
import static com.github.chhsiao90.nitmproxy.testing.EmbeddedChannelAssert.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class Http1BackendHandlerTest {
    private Http1BackendHandler handler;

    private EmbeddedChannel channel;

    @Before
    public void setUp() {
        HandlerProvider provider = mock(HandlerProvider.class);
        when(provider.wsBackendHandler()).thenReturn(new ChannelHandlerAdapter() {});
        when(provider.tailBackendHandler()).thenReturn(new ChannelHandlerAdapter() {});

        NitmProxyMaster master = mock(NitmProxyMaster.class);
        when(master.config()).thenReturn(new NitmProxyConfig());
        when(master.provider(any())).thenReturn(provider);
        when(master.listenerProvider()).thenReturn(NitmProxyListenerProvider.empty());

        channel = new EmbeddedChannel();

        ConnectionContext context = new ConnectionContext(master)
                .withClientAddr(new Address("localhost", 8080))
                .withClientChannel(channel);
        handler = new Http1BackendHandler(context);
    }

    @After
    public void tearDown() {
        channel.finishAndReleaseAll();
    }

    @Test
    public void shouldHandleRequestAndResponse() {
        channel.pipeline().addLast(handler);

        assertTrue(channel.writeOutbound(defaultRequest()));
        assertChannel(channel)
                .hasOutboundMessage()
                .hasByteBuf()
                .hasContent("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n")
                .release();

        assertTrue(channel.writeInbound(defaultResponse("test")));
        assertChannel(channel)
                .hasInboundMessage()
                .hasResponse()
                .release();
    }

    @Test
    public void shouldHandleRequestsAndResponses() {
        channel.pipeline().addLast(handler);

        // First request
        assertTrue(channel.writeOutbound(defaultRequest()));
        assertChannel(channel)
                .hasOutboundMessage()
                .hasByteBuf()
                .hasContent("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n")
                .release();

        // First response
        assertTrue(channel.writeInbound(defaultResponse("test")));
        assertChannel(channel)
                .hasInboundMessage()
                .hasResponse()
                .release();

        // Second request
        assertTrue(channel.writeOutbound(defaultRequest()));
        assertChannel(channel)
                .hasOutboundMessage()
                .hasByteBuf()
                .hasContent("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n")
                .release();

        // Second response
        assertTrue(channel.writeInbound(defaultResponse("test")));
        assertChannel(channel)
                .hasInboundMessage()
                .hasResponse()
                .release();
    }
}
