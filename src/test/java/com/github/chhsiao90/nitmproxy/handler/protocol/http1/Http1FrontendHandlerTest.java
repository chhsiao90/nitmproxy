package com.github.chhsiao90.nitmproxy.handler.protocol.http1;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.HandlerProvider;
import com.github.chhsiao90.nitmproxy.HttpObjectUtil;
import com.github.chhsiao90.nitmproxy.NitmProxyConfig;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.netty.util.ReferenceCountUtil.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class Http1FrontendHandlerTest {
    private NitmProxyMaster master;

    private EmbeddedChannel inboundChannel;

    private EmbeddedChannel outboundChannel;

    @Before
    public void setUp() throws Exception {
        master = mock(NitmProxyMaster.class);
        HandlerProvider provider = mock(HandlerProvider.class);
        when(master.config()).thenReturn(new NitmProxyConfig());
        when(master.provider(any())).thenReturn(provider);
        when(provider.http1EventHandler()).thenReturn(new ChannelHandlerAdapter() {});
        when(provider.http1BackendHandler()).thenReturn(new ChannelHandlerAdapter() {});
        when(provider.tlsFrontendHandler()).thenReturn(new ChannelHandlerAdapter() {});

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

        assertFalse(inboundChannel.writeInbound(HttpObjectUtil.fullRequestAsBytes()));

        assertEquals(1, outboundChannel.outboundMessages().size());
        assertTrue(outboundChannel.outboundMessages().peek() instanceof FullHttpRequest);

        FullHttpRequest request = (FullHttpRequest) outboundChannel.outboundMessages().poll();
        assertEquals(HttpMethod.GET, request.method());
        assertEquals(HttpVersion.HTTP_1_1, request.protocolVersion());
        assertEquals("/", request.uri());
        assertEquals("localhost", request.headers().get(HttpHeaderNames.HOST));
        assertEquals(0, request.content().readableBytes());
        release(request);
    }

    @Test
    public void shouldTunnelRequests() {
        Http1FrontendHandler handler = tunneledHandler();
        inboundChannel.pipeline().addLast(handler);

        assertFalse(inboundChannel.writeInbound(HttpObjectUtil.fullRequestAsBytes()));

        assertEquals(1, outboundChannel.outboundMessages().size());
        assertTrue(outboundChannel.outboundMessages().peek() instanceof FullHttpRequest);
        release(outboundChannel.outboundMessages().poll());

        assertFalse(inboundChannel.writeInbound(HttpObjectUtil.fullRequestAsBytes()));
        assertTrue(outboundChannel.outboundMessages().peek() instanceof FullHttpRequest);
        release(outboundChannel.outboundMessages().poll());
    }

    @Test
    public void shouldHandleHttpProxyRequest() {
        Http1FrontendHandler handler = httpProxyHandler(true);
        inboundChannel.pipeline().addLast(handler);

        ByteBuf requestBytes = HttpObjectUtil.fullRequestAsBytes(new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost:9000/"));
        assertFalse(inboundChannel.writeInbound(requestBytes));

        assertEquals(1, outboundChannel.outboundMessages().size());
        assertTrue(outboundChannel.outboundMessages().peek() instanceof FullHttpRequest);

        HttpRequest httpRequest = (HttpRequest) outboundChannel.outboundMessages().poll();
        assertEquals(httpRequest.method(), HttpMethod.GET);
        assertEquals(httpRequest.protocolVersion(), HttpVersion.HTTP_1_1);
        assertEquals(httpRequest.uri(), "/");
        release(httpRequest);
    }

    @Test
    public void shouldHandleHttpProxyRequests() {
        Http1FrontendHandler handler = httpProxyHandler(true);
        inboundChannel.pipeline().addLast(handler);

        ByteBuf requestBytes = HttpObjectUtil.fullRequestAsBytes(new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost:9000/"));
        assertFalse(inboundChannel.writeInbound(requestBytes.copy()));

        assertEquals(1, outboundChannel.outboundMessages().size());
        assertTrue(outboundChannel.outboundMessages().peek() instanceof FullHttpRequest);

        HttpRequest httpRequest = (HttpRequest) outboundChannel.outboundMessages().poll();
        assertEquals(httpRequest.method(), HttpMethod.GET);
        assertEquals(httpRequest.protocolVersion(), HttpVersion.HTTP_1_1);
        assertEquals(httpRequest.uri(), "/");
        release(httpRequest);

        // Second request
        assertFalse(inboundChannel.writeInbound(requestBytes));

        assertEquals(1, outboundChannel.outboundMessages().size());
        assertTrue(outboundChannel.outboundMessages().peek() instanceof FullHttpRequest);

        httpRequest = (HttpRequest) outboundChannel.outboundMessages().poll();
        assertEquals(httpRequest.method(), HttpMethod.GET);
        assertEquals(httpRequest.protocolVersion(), HttpVersion.HTTP_1_1);
        assertEquals(httpRequest.uri(), "/");
        release(httpRequest);
    }

    @Test
    public void shouldHandleHttpProxyCreateNewConnection() {
        Http1FrontendHandler handler = httpProxyHandler(true);
        inboundChannel.pipeline().addLast(handler);

        ByteBuf firstRequestBytes = HttpObjectUtil.fullRequestAsBytes(new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost:8000/"));
        assertFalse(inboundChannel.writeInbound(firstRequestBytes));

        assertEquals(1, outboundChannel.outboundMessages().size());
        assertTrue(outboundChannel.outboundMessages().peek() instanceof FullHttpRequest);

        HttpRequest httpRequest = (HttpRequest) outboundChannel.outboundMessages().poll();
        assertEquals(httpRequest.method(), HttpMethod.GET);
        assertEquals(httpRequest.protocolVersion(), HttpVersion.HTTP_1_1);
        assertEquals(httpRequest.uri(), "/");
        release(httpRequest);

        EmbeddedChannel firstOutboundChannel = outboundChannel;

        // Second request
        ByteBuf secondRequestBytes = HttpObjectUtil.fullRequestAsBytes(new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost:9000/"));
        assertFalse(inboundChannel.writeInbound(secondRequestBytes));

        assertEquals(1, outboundChannel.outboundMessages().size());
        assertTrue(outboundChannel.outboundMessages().peek() instanceof FullHttpRequest);

        httpRequest = (HttpRequest) outboundChannel.outboundMessages().poll();
        assertEquals(httpRequest.method(), HttpMethod.GET);
        assertEquals(httpRequest.protocolVersion(), HttpVersion.HTTP_1_1);
        assertEquals(httpRequest.uri(), "/");
        release(httpRequest);

        assertNotSame(firstOutboundChannel, outboundChannel);
        assertFalse(firstOutboundChannel.isActive());
    }

    @Test
    public void shouldClosedWhenHttpProxyDestinationNotAvailable() {
        Http1FrontendHandler handler = httpProxyHandler(false);
        inboundChannel.pipeline().addLast(handler);

        ByteBuf firstRequestBytes = HttpObjectUtil.fullRequestAsBytes(new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost:8000/"));
        assertFalse(inboundChannel.writeInbound(firstRequestBytes));

        assertFalse(inboundChannel.isActive());
    }

    @Test
    public void shouldCreateNewOutboundWhenOldIsInactive() {
        Http1FrontendHandler handler = httpProxyHandler(true);
        inboundChannel.pipeline().addLast(handler);

        ByteBuf firstRequestBytes = HttpObjectUtil.fullRequestAsBytes(new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost:8000/"));
        assertFalse(inboundChannel.writeInbound(firstRequestBytes));

        assertEquals(1, outboundChannel.outboundMessages().size());
        assertTrue(outboundChannel.outboundMessages().peek() instanceof FullHttpRequest);

        HttpRequest httpRequest = (HttpRequest) outboundChannel.outboundMessages().poll();
        assertEquals(httpRequest.method(), HttpMethod.GET);
        assertEquals(httpRequest.protocolVersion(), HttpVersion.HTTP_1_1);
        assertEquals(httpRequest.uri(), "/");
        release(httpRequest);

        EmbeddedChannel firstOutboundChannel = outboundChannel;
        outboundChannel.close();

        // Second request
        ByteBuf secondRequestBytes = HttpObjectUtil.fullRequestAsBytes(new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost:8000/"));
        assertFalse(inboundChannel.writeInbound(secondRequestBytes));

        assertEquals(1, outboundChannel.outboundMessages().size());
        assertTrue(outboundChannel.outboundMessages().peek() instanceof FullHttpRequest);

        httpRequest = (HttpRequest) outboundChannel.outboundMessages().poll();
        assertEquals(httpRequest.method(), HttpMethod.GET);
        assertEquals(httpRequest.protocolVersion(), HttpVersion.HTTP_1_1);
        assertEquals(httpRequest.uri(), "/");
        release(httpRequest);

        assertNotSame(firstOutboundChannel, outboundChannel);
    }

    @Test
    public void shouldHandleConnect() {
        Http1FrontendHandler handler = httpProxyHandler(true);
        inboundChannel.pipeline().addLast(handler);

        ByteBuf requestBytes = HttpObjectUtil.fullRequestAsBytes(new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.CONNECT, "localhost:8000"));
        assertFalse(inboundChannel.writeInbound(requestBytes));

        assertNotNull(outboundChannel);
        assertTrue(outboundChannel.isActive());

        assertEquals(1, inboundChannel.outboundMessages().size());
        assertTrue(inboundChannel.outboundMessages().peek() instanceof ByteBuf);

        ByteBuf respByteBuf = (ByteBuf) inboundChannel.outboundMessages().poll();
        byte[] respBytes = new byte[respByteBuf.readableBytes()];
        respByteBuf.readBytes(respBytes);
        assertEquals("HTTP/1.1 200 OK\r\n\r\n", new String(respBytes));
        respByteBuf.release();
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
