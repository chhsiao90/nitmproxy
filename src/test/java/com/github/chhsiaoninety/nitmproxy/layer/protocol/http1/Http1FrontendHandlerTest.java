package com.github.chhsiaoninety.nitmproxy.layer.protocol.http1;

import com.github.chhsiaoninety.nitmproxy.Address;
import com.github.chhsiaoninety.nitmproxy.ConnectionInfo;
import com.github.chhsiaoninety.nitmproxy.NitmProxyConfig;
import com.github.chhsiaoninety.nitmproxy.NitmProxyMaster;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
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
                invocationOnMock -> new EmbeddedChannel((ChannelHandler) invocationOnMock.getArguments()[2]));

        inboundChannel = new EmbeddedChannel();

        outboundChannel = new EmbeddedChannel();
    }

    @After
    public void tearDown() {
        inboundChannel.finishAndReleaseAll();
        outboundChannel.finishAndReleaseAll();
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

    private Http1FrontendHandler httpProxyHandler() {
        return new Http1FrontendHandler(master, connectionInfo());
    }

    private Http1FrontendHandler tunneledHandler() {
        return new Http1FrontendHandler(master, connectionInfo(), outboundChannel);
    }

    private static ConnectionInfo connectionInfo() {
        return new ConnectionInfo(
                new Address("localhost", 8080),
                new Address("localhost", 8080));
    }
}
