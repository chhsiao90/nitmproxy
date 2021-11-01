package com.github.chhsiao90.nitmproxy.handler.protocol.ws;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.HandlerProvider;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.listener.NitmProxyListenerProvider;
import com.github.chhsiao90.nitmproxy.testing.EmptyChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.github.chhsiao90.nitmproxy.testing.EmbeddedChannelAssert.*;
import static io.netty.handler.codec.http.HttpVersion.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WebSocketFrontendHandlerTest {

    private ConnectionContext context;
    private EmbeddedChannel channel;

    @Before
    public void setUp() {
        HandlerProvider provider = mock(HandlerProvider.class);
        when(provider.wsEventHandler()).thenReturn(EmptyChannelHandler.empty());

        NitmProxyMaster master = mock(NitmProxyMaster.class);
        when(master.provider(any())).thenReturn(provider);
        when(master.listenerProvider()).thenReturn(NitmProxyListenerProvider.empty());

        context = new ConnectionContext(master)
                .withClientAddr(new Address("localhost", 8888));

        channel = new EmbeddedChannel(new WebSocketFrontendHandler(context));
    }

    @After
    public void tearDown() {
        channel.finishAndReleaseAll();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHandshake() {
        assertTrue(channel.writeInbound(handshakeRequest()));

        assertChannel(channel)
                .hasInboundMessage()
                .hasRequest()
                .release();
        assertChannel(channel)
                .pipeline()
                .hasHandlers(WebSocketFrontendHandler.class,
                        WebSocketServerCompressionHandler.class,
                        EmptyChannelHandler.class);
        assertEquals("/chat", context.wsCtx().path());

        assertTrue(channel.writeOutbound(handshakeResponse(null)));

        assertChannel(channel)
                .hasOutboundMessage()
                .hasResponse()
                .release();
        assertChannel(channel)
                .pipeline()
                .hasHandlers(WebSocket13FrameEncoder.class,
                        WebSocket13FrameDecoder.class,
                        WebSocketFrontendHandler.class,
                        EmptyChannelHandler.class);
    }

    private static FullHttpRequest handshakeRequest() {
        FullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, HttpMethod.GET, "/chat");
        request.headers().set(HttpHeaderNames.HOST, "server.example.com");
        request.headers().set(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET);
        request.headers().set(HttpHeaderNames.CONNECTION, "Upgrade");
        request.headers().set(HttpHeaderNames.SEC_WEBSOCKET_KEY, "dGhlIHNhbXBsZSBub25jZQ==");
        request.headers().set(HttpHeaderNames.SEC_WEBSOCKET_ORIGIN, "http://example.com");
        request.headers().set(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, "chat, superchat");
        request.headers().set(HttpHeaderNames.SEC_WEBSOCKET_VERSION, "13");
        return request;
    }

    private static FullHttpResponse handshakeResponse(String extension) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.SWITCHING_PROTOCOLS);
        response.headers().set(HttpHeaderNames.HOST, "server.example.com");
        response.headers().set(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET.toString().toLowerCase());
        response.headers().set(HttpHeaderNames.CONNECTION, "Upgrade");
        response.headers().set(HttpHeaderNames.ORIGIN, "http://example.com");
        if (extension != null) {
            response.headers().set(HttpHeaderNames.SEC_WEBSOCKET_EXTENSIONS, extension);
        }
        return response;
    }
}
