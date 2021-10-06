package com.github.chhsiao90.nitmproxy.handler.protocol.http1;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.HandlerProvider;
import com.github.chhsiao90.nitmproxy.NitmProxyConfig;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class Http1BackendHandlerTest {
    private Http1BackendHandler handler;

    private EmbeddedChannel inboundChannel;

    private EmbeddedChannel outboundChannel;

    @Before
    public void setUp() throws Exception {
        HandlerProvider provider = mock(HandlerProvider.class);
        when(provider.wsBackendHandler()).thenReturn(new ChannelHandlerAdapter() {});

        NitmProxyMaster master = mock(NitmProxyMaster.class);
        when(master.config()).thenReturn(new NitmProxyConfig());
        when(master.provider(any())).thenReturn(provider);

        inboundChannel = new EmbeddedChannel();

        outboundChannel = new EmbeddedChannel();

        ConnectionContext context = new ConnectionContext(master)
                .withClientAddr(new Address("localhost", 8080))
                .withClientChannel(outboundChannel)
                .withServerAddr(new Address("localhost", 8080))
                .withServerChannel(inboundChannel);
        handler = new Http1BackendHandler(context);
    }

    @After
    public void tearDown() {
        inboundChannel.finishAndReleaseAll();
        outboundChannel.finishAndReleaseAll();
    }

    @Test
    public void shouldHandlerRequestAndResponse() {
        inboundChannel.pipeline().addLast(handler);

        DefaultFullHttpRequest req = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        inboundChannel.writeAndFlush(req);

        assertEquals(1, inboundChannel.outboundMessages().size());

        Object outboundReq = inboundChannel.outboundMessages().poll();
        assertTrue(outboundReq instanceof ByteBuf);
        assertEquals("GET / HTTP/1.1\r\n\r\n",
                new String(readBytes((ByteBuf) outboundReq), US_ASCII));

        DefaultFullHttpResponse resp = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        assertFalse(inboundChannel.writeInbound(resp));

        assertEquals(1, outboundChannel.outboundMessages().size());
        assertEquals(resp, outboundChannel.outboundMessages().poll());

        resp.release();
    }

    @Test
    public void shouldHandleRequestsAndResponses() {
        inboundChannel.pipeline().addLast(handler);

        DefaultFullHttpRequest req = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/");

        // First request
        inboundChannel.writeAndFlush(req.retain());

        assertEquals(1, inboundChannel.outboundMessages().size());
        assertTrue(inboundChannel.outboundMessages().poll() instanceof ByteBuf);

        // First response
        DefaultFullHttpResponse resp = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        assertFalse(inboundChannel.writeInbound(resp));

        assertEquals(1, outboundChannel.outboundMessages().size());
        assertEquals(resp, outboundChannel.outboundMessages().poll());

        // Second request
        inboundChannel.writeAndFlush(req);

        assertEquals(1, inboundChannel.outboundMessages().size());
        assertTrue(inboundChannel.outboundMessages().poll() instanceof ByteBuf);

        // Second response
        assertFalse(inboundChannel.writeInbound(resp));

        assertEquals(1, outboundChannel.outboundMessages().size());
        assertEquals(resp, outboundChannel.outboundMessages().poll());

        resp.release();
    }

    private static byte[] readBytes(ByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        return bytes;
    }
}
