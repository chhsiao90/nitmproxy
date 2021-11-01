package com.github.chhsiao90.nitmproxy.handler.protocol.http1;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.HandlerProvider;
import com.github.chhsiao90.nitmproxy.NitmProxyConfig;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.listener.NitmProxyListenerProvider;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.github.chhsiao90.nitmproxy.http.HttpUtil.*;
import static com.github.chhsiao90.nitmproxy.testing.EmbeddedChannelAssert.*;
import static com.google.common.net.HttpHeaders.*;
import static io.netty.handler.codec.http.HttpMethod.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class Http1FrontendHandlerTest {
    private NitmProxyMaster master;

    private EmbeddedChannel inboundChannel;

    private EmbeddedChannel outboundChannel;

    @Before
    public void setUp() {
        master = mock(NitmProxyMaster.class);
        HandlerProvider provider = mock(HandlerProvider.class);
        when(master.config()).thenReturn(new NitmProxyConfig());
        when(master.provider(any())).thenReturn(provider);
        when(master.listenerProvider()).thenReturn(NitmProxyListenerProvider.empty());
        when(provider.http1EventHandler()).thenReturn(new ChannelHandlerAdapter() {});
        when(provider.tlsFrontendHandler()).thenReturn(new ChannelHandlerAdapter() {});
        when(provider.wsFrontendHandler()).thenReturn(new ChannelHandlerAdapter() {});

        inboundChannel = new EmbeddedChannel();
    }

    @After
    public void tearDown() {
        inboundChannel.finishAndReleaseAll();

        if (outboundChannel != null) {
            outboundChannel.finishAndReleaseAll();
        }
    }

    @Test
    public void shouldTunnelRequest() {
        Http1FrontendHandler handler = tunneledHandler();
        inboundChannel.pipeline().addLast(handler);

        assertTrue(inboundChannel.writeInbound(toBytes(defaultRequest())));
        assertChannel(inboundChannel)
                .hasInboundMessage()
                .hasRequest()
                .hasMethod(GET)
                .hasUrl("/")
                .hasHeader(HOST, "localhost")
                .hasContent("")
                .release();
    }

    @Test
    public void shouldTunnelRequests() {
        Http1FrontendHandler handler = tunneledHandler();
        inboundChannel.pipeline().addLast(handler);

        assertTrue(inboundChannel.writeInbound(toBytes(defaultRequest())));
        assertChannel(inboundChannel)
                .hasInboundMessage()
                .hasRequest()
                .hasMethod(GET)
                .hasUrl("/")
                .hasHeader(HOST, "localhost")
                .hasContent("")
                .release();

        assertTrue(inboundChannel.writeInbound(toBytes(defaultRequest())));
        assertChannel(inboundChannel)
                .hasInboundMessage()
                .hasRequest()
                .hasMethod(GET)
                .hasUrl("/")
                .hasHeader(HOST, "localhost")
                .hasContent("")
                .release();
    }

    @Test
    public void shouldHandleHttpProxyRequest() {
        Http1FrontendHandler handler = httpProxyHandler(true);
        inboundChannel.pipeline().addLast(handler);

        ByteBuf requestBytes = toBytes(request(
                HttpVersion.HTTP_1_1, GET, "localhost:9000", "http://localhost:9000/"));
        assertTrue(inboundChannel.writeInbound(requestBytes));

        assertChannel(inboundChannel)
                .hasInboundMessage()
                .hasRequest()
                .hasMethod(GET)
                .hasUrl("/")
                .hasHeader(HOST, "localhost:9000")
                .hasContent("")
                .release();
    }

    @Test
    public void shouldHandleHttpProxyRequests() {
        Http1FrontendHandler handler = httpProxyHandler(true);
        inboundChannel.pipeline().addLast(handler);

        ByteBuf requestBytes = toBytes(request(
                HttpVersion.HTTP_1_1, GET, "localhost:9000", "http://localhost:9000/"));
        assertTrue(inboundChannel.writeInbound(requestBytes.copy()));
        assertChannel(inboundChannel)
                .hasInboundMessage()
                .hasRequest()
                .hasMethod(GET)
                .hasUrl("/")
                .hasHeader(HOST, "localhost:9000")
                .hasContent("")
                .release();

        // Second request
        assertTrue(inboundChannel.writeInbound(requestBytes));
        assertChannel(inboundChannel)
                .hasInboundMessage()
                .hasRequest()
                .hasMethod(GET)
                .hasUrl("/")
                .hasHeader(HOST, "localhost:9000")
                .hasContent("")
                .release();
    }

    @Test
    public void shouldHandleHttpProxyCreateNewConnection() {
        Http1FrontendHandler handler = httpProxyHandler(true);
        inboundChannel.pipeline().addLast(handler);

        ByteBuf firstRequestBytes = toBytes(request(
                HttpVersion.HTTP_1_1, GET, "localhost:8000", "http://localhost:8000/"));

        assertTrue(inboundChannel.writeInbound(firstRequestBytes));
        assertChannel(inboundChannel)
                .hasInboundMessage()
                .hasRequest()
                .hasMethod(GET)
                .hasUrl("/")
                .hasHeader(HOST, "localhost:8000")
                .hasContent("")
                .release();

        EmbeddedChannel firstOutboundChannel = outboundChannel;

        // Second request
        ByteBuf secondRequestBytes = toBytes(request(
                HttpVersion.HTTP_1_1, GET, "localhost:9000", "http://localhost:9000/"));
        assertTrue(inboundChannel.writeInbound(secondRequestBytes));
        assertChannel(inboundChannel)
                .hasInboundMessage()
                .hasRequest()
                .hasMethod(GET)
                .hasUrl("/")
                .hasHeader(HOST, "localhost:9000")
                .hasContent("")
                .release();

        assertNotSame(firstOutboundChannel, outboundChannel);
        assertFalse(firstOutboundChannel.isActive());
    }

    @Test
    public void shouldClosedWhenHttpProxyDestinationNotAvailable() {
        Http1FrontendHandler handler = httpProxyHandler(false);
        inboundChannel.pipeline().addLast(handler);

        ByteBuf requestBytes = toBytes(request(
                HttpVersion.HTTP_1_1, GET, "localhost:8000", "http://localhost:8000/"));
        assertFalse(inboundChannel.writeInbound(requestBytes));

        assertFalse(inboundChannel.isActive());
    }

    @Test
    public void shouldCreateNewOutboundWhenOldIsInactive() {
        Http1FrontendHandler handler = httpProxyHandler(true);
        inboundChannel.pipeline().addLast(handler);

        ByteBuf firstRequestBytes = toBytes(request(
                HttpVersion.HTTP_1_1, GET, "localhost:8000", "http://localhost:8000/"));
        assertTrue(inboundChannel.writeInbound(firstRequestBytes));
        assertChannel(inboundChannel)
                .hasInboundMessage()
                .hasRequest()
                .release();

        EmbeddedChannel firstOutboundChannel = outboundChannel;
        outboundChannel.close();

        // Second request
        ByteBuf secondRequestBytes = toBytes(request(
                HttpVersion.HTTP_1_1, GET, "localhost:8000", "http://localhost:8000/"));
        assertTrue(inboundChannel.writeInbound(secondRequestBytes));
        assertChannel(inboundChannel)
                .hasInboundMessage()
                .hasRequest()
                .release();

        assertNotSame(firstOutboundChannel, outboundChannel);
    }

    @Test
    public void shouldHandleConnect() {
        Http1FrontendHandler handler = httpProxyHandler(true);
        inboundChannel.pipeline().addLast(handler);

        ByteBuf requestBytes = toBytes(request(
                HttpVersion.HTTP_1_1, HttpMethod.CONNECT, "localhost:8000", "http://localhost:8000/"));
        assertFalse(inboundChannel.writeInbound(requestBytes));

        assertNotNull(outboundChannel);
        assertTrue(outboundChannel.isActive());

        assertChannel(inboundChannel)
                .hasOutboundMessage()
                .hasByteBuf()
                .hasContent("HTTP/1.1 200 OK\r\n\r\n")
                .release();
    }

    private Http1FrontendHandler httpProxyHandler(boolean outboundAvailable) {
        if (outboundAvailable) {
            when(master.connect(any(), any(), any())).then(
                    invocationOnMock ->  {
                        outboundChannel = new EmbeddedChannel((ChannelHandler) invocationOnMock.getArguments()[2]);
                        return outboundChannel.newSucceededFuture();
                    });
        } else {
            when(master.connect(any(), any(), any())).then(
                    invocationOnMock ->  inboundChannel.newPromise().setFailure(new Exception()));
        }
        return new Http1FrontendHandler(master, createConnectionContext());
    }

    private Http1FrontendHandler tunneledHandler() {
        outboundChannel = new EmbeddedChannel();
        return new Http1FrontendHandler(master, createConnectionContext());
    }

    private ConnectionContext createConnectionContext() {
        ConnectionContext context = new ConnectionContext(master)
                .withClientAddr(new Address("localhost", 8080))
                .withClientChannel(inboundChannel);
        if (outboundChannel != null) {
            context.withServerAddr(new Address("localhost", 8080))
                    .withServerChannel(outboundChannel);
        }
        return context;
    }
}
