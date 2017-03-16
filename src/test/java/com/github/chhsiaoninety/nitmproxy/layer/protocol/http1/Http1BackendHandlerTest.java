package com.github.chhsiaoninety.nitmproxy.layer.protocol.http1;

import com.github.chhsiaoninety.nitmproxy.Address;
import com.github.chhsiaoninety.nitmproxy.ConnectionInfo;
import com.github.chhsiaoninety.nitmproxy.NitmProxyConfig;
import com.github.chhsiaoninety.nitmproxy.event.OutboundChannelClosedEvent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class Http1BackendHandlerTest {
    private Http1BackendHandler handler;

    private EmbeddedChannel inboundChannel;

    private EmbeddedChannel outboundChannel;

    @Before
    public void setUp() throws Exception {
        inboundChannel = new EmbeddedChannel();

        outboundChannel = new EmbeddedChannel();

        handler = new Http1BackendHandler(
                new NitmProxyConfig(),
                connectionInfo(),
                outboundChannel);
    }

    @After
    public void tearDown() {
        inboundChannel.finishAndReleaseAll();
        outboundChannel.finishAndReleaseAll();
    }

    @Test
    public void shouldFireOutboundChannelClosedEvent() throws InterruptedException {
        inboundChannel.pipeline().addLast(handler);

        List<Object> events = new ArrayList<>(1);
        outboundChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                events.add(evt);
            }
        });

        inboundChannel.close().sync();

        assertFalse(events.isEmpty());
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof OutboundChannelClosedEvent);
    }

    @Test
    public void shouldHandlerRequestAndResponse() {
        inboundChannel.pipeline().addLast(handler);

        DefaultFullHttpRequest req = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        inboundChannel.write(req);

        assertEquals(1, inboundChannel.outboundMessages().size());

        Object outboundReq = inboundChannel.outboundMessages().poll();
        assertTrue(outboundReq instanceof ByteBuf);
        assertEquals("GET / HTTP/1.1\r\n\r\n", new String(readBytes((ByteBuf) outboundReq)));

        DefaultFullHttpResponse resp = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        assertFalse(inboundChannel.writeInbound(resp));

        assertEquals(1, outboundChannel.outboundMessages().size());
        assertEquals(resp, outboundChannel.outboundMessages().poll());

        resp.release();
    }

    @Test
    public void shouldPendingRequests() {
        inboundChannel.pipeline().addLast(handler);

        DefaultFullHttpRequest req = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        // First request
        inboundChannel.write(req.retain());

        assertEquals(1, inboundChannel.outboundMessages().size());
        assertTrue(inboundChannel.outboundMessages().poll() instanceof ByteBuf);

        // Second request
        inboundChannel.write(req);

        // Should pending second request
        assertTrue(inboundChannel.outboundMessages().isEmpty());
    }

    @Test
    public void shouldHandleRequestsAndResponses() {
        inboundChannel.pipeline().addLast(handler);

        DefaultFullHttpRequest req = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/");

        // First request
        inboundChannel.write(req.retain());

        assertEquals(1, inboundChannel.outboundMessages().size());
        assertTrue(inboundChannel.outboundMessages().poll() instanceof ByteBuf);

        // First response
        DefaultFullHttpResponse resp = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        assertFalse(inboundChannel.writeInbound(resp));

        assertEquals(1, outboundChannel.outboundMessages().size());
        assertEquals(resp, outboundChannel.outboundMessages().poll());

        // Second request
        inboundChannel.write(req);

        assertEquals(1, inboundChannel.outboundMessages().size());
        assertTrue(inboundChannel.outboundMessages().poll() instanceof ByteBuf);

        // Second response
        assertFalse(inboundChannel.writeInbound(resp));

        assertEquals(1, outboundChannel.outboundMessages().size());
        assertEquals(resp, outboundChannel.outboundMessages().poll());

        resp.release();
    }

    @Test
    public void shouldClearPendingRequests() {
        inboundChannel.pipeline().addLast(handler);

        DefaultFullHttpRequest req = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        // First request
        inboundChannel.write(req.retain());

        assertEquals(1, req.refCnt());
        assertEquals(1, inboundChannel.outboundMessages().size());
        assertTrue(inboundChannel.outboundMessages().poll() instanceof ByteBuf);

        // Second request
        inboundChannel.write(req);

        assertEquals(1, req.refCnt());
        assertTrue(inboundChannel.outboundMessages().isEmpty());

        inboundChannel.close();

        assertEquals(0, req.refCnt());
    }

    private static ConnectionInfo connectionInfo() {
        return new ConnectionInfo(
                new Address("localhost", 8080),
                new Address("localhost", 8080));
    }

    private static byte[] readBytes(ByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        return bytes;
    }
}
