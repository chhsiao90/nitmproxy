package com.github.chhsiaoninety.nitmproxy.handler.protocol.http1;

import com.github.chhsiaoninety.nitmproxy.Address;
import com.github.chhsiaoninety.nitmproxy.ConnectionInfo;
import com.github.chhsiaoninety.nitmproxy.NitmProxyConfig;
import com.github.chhsiaoninety.nitmproxy.NitmProxyMaster;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.github.chhsiaoninety.nitmproxy.HttpObjectUtil.*;
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
        when(master.config()).thenReturn(new NitmProxyConfig());
        when(master.connect(any(), any(), any())).then(
                invocationOnMock ->  {
                    outboundChannel = new EmbeddedChannel((ChannelHandler) invocationOnMock.getArguments()[2]);
                    return outboundChannel.newSucceededFuture();
                });
        when(master.handler(any(), any(), any())).thenAnswer(m -> new ChannelHandlerAdapter() {
        });

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

        assertFalse(inboundChannel.writeInbound(requestBytes()));

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

        assertFalse(inboundChannel.writeInbound(requestBytes()));

        assertEquals(1, outboundChannel.outboundMessages().size());
        assertTrue(outboundChannel.outboundMessages().peek() instanceof FullHttpRequest);
        release(outboundChannel.outboundMessages().poll());

        assertFalse(inboundChannel.writeInbound(requestBytes()));
        assertTrue(outboundChannel.outboundMessages().peek() instanceof FullHttpRequest);
        release(outboundChannel.outboundMessages().poll());
    }

    @Test
    public void shouldHandleHttpProxyRequest() {
        Http1FrontendHandler handler = httpProxyHandler();
        inboundChannel.pipeline().addLast(handler);

        ByteBuf requestBytes = requestBytes(new DefaultHttpRequest(
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
        Http1FrontendHandler handler = httpProxyHandler();
        inboundChannel.pipeline().addLast(handler);

        ByteBuf requestBytes = requestBytes(new DefaultHttpRequest(
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
        Http1FrontendHandler handler = httpProxyHandler();
        inboundChannel.pipeline().addLast(handler);

        ByteBuf firstRequestBytes = requestBytes(new DefaultHttpRequest(
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
        ByteBuf secondRequestBytes = requestBytes(new DefaultHttpRequest(
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

    private Http1FrontendHandler httpProxyHandler() {
        return new Http1FrontendHandler(master, connectionInfo());
    }

    private Http1FrontendHandler tunneledHandler() {
        outboundChannel = new EmbeddedChannel();
        return new Http1FrontendHandler(master, connectionInfo(), outboundChannel);
    }

    private static ConnectionInfo connectionInfo() {
        return new ConnectionInfo(
                new Address("localhost", 8080),
                new Address("localhost", 8080));
    }
}
