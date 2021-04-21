package com.github.chhsiao90.nitmproxy.handler.protocol.http2;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyConfig;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.testing.EmbeddedChannelAssert;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2ConnectionPrefaceAndSettingsFrameWrittenEvent;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2FrameAdapter;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2SettingsFrame;
import io.netty.util.AsciiString;
import io.netty.util.concurrent.Future;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FrameWrapper.*;
import static io.netty.buffer.ByteBufUtil.*;
import static io.netty.util.ReferenceCountUtil.*;
import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class Http2FrontendHandlerTest {

    private static final long DEFAULT_AWAIT_TIMEOUT_SECONDS = 15;

    private ConnectionContext connectionContext;

    private Http2ConnectionHandler http2Client;
    private ServerBootstrap sb;
    private Bootstrap cb;
    private Channel serverChannel;
    private volatile Channel serverConnectedChannel;
    private Channel clientChannel;

    private EmbeddedChannel targetChannel;

    @Before
    public void setUp() throws Exception {
        NitmProxyMaster master = mock(NitmProxyMaster.class);
        when(master.config()).thenReturn(new NitmProxyConfig());

        targetChannel = new EmbeddedChannel();
        connectionContext = new ConnectionContext(master)
                .withServerAddr(new Address("localhost", 8080))
                .withServerChannel(targetChannel);
    }

    @After
    public void tearDown() {
        targetChannel.finishAndReleaseAll();
        if (clientChannel != null) {
            clientChannel.close().syncUninterruptibly();
            clientChannel = null;
        }
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
            serverChannel = null;
        }
        Channel serverConnectedChannel = this.serverConnectedChannel;
        if (serverConnectedChannel != null) {
            serverConnectedChannel.close().syncUninterruptibly();
            this.serverConnectedChannel = null;
        }
        Future<?> serverGroup = sb.config().group().shutdownGracefully(0, 5, SECONDS);
        Future<?> serverChildGroup = sb.config().childGroup().shutdownGracefully(0, 5, SECONDS);
        Future<?> clientGroup = cb.config().group().shutdownGracefully(0, 5, SECONDS);
        serverGroup.syncUninterruptibly();
        serverChildGroup.syncUninterruptibly();
        clientGroup.syncUninterruptibly();
    }

    @Test
    public void shouldSendHeaders() throws Exception {
        bootstrapEnv();
        CountDownLatch latch = new CountDownLatch(1);
        targetChannel.pipeline().addLast(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
                    throws Exception {
                super.write(ctx, msg, promise);
                if (isFrame(msg, Http2HeadersFrame.class)) {
                    latch.countDown();
                }
            }
        });

        ChannelHandlerContext ctx = clientChannel.pipeline().firstContext();
        Http2TestUtil.runInChannel(clientChannel, () -> {
            http2Client.encoder().writeHeaders(ctx, 1, dummyHeaders(), 0, true, ctx.newPromise());
            http2Client.flush(ctx);
        });

        assertTrue(latch.await(DEFAULT_AWAIT_TIMEOUT_SECONDS, SECONDS));
        EmbeddedChannelAssert.assertChannel(targetChannel).hasOutboundMessage().hasSize(2);

        assertThat(targetChannel.outboundMessages().poll())
                .isInstanceOf(Http2FrameWrapper.class)
                .satisfies(wrapper -> assertThat(frame(wrapper)).isInstanceOf(Http2SettingsFrame.class));
        assertThat(targetChannel.outboundMessages().poll())
                .isInstanceOf(Http2FrameWrapper.class)
                .satisfies(wrapper -> assertThat(frame(wrapper)).isInstanceOf(Http2HeadersFrame.class));
    }

    @Test
    public void shouldSendHeadersWithData() throws Exception {
        bootstrapEnv();
        CountDownLatch latch = new CountDownLatch(1);
        targetChannel.pipeline().addLast(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
                    throws Exception {
                super.write(ctx, msg, promise);
                if (isFrame(msg, Http2DataFrame.class)) {
                    latch.countDown();
                }
            }
        });

        ChannelHandlerContext ctx = clientChannel.pipeline().firstContext();
        Http2TestUtil.runInChannel(clientChannel, () -> {
            http2Client.encoder().writeHeaders(ctx, 1, dummyHeaders(), 0, false, ctx.newPromise());
            http2Client.encoder().writeData(
                    ctx, 1, writeUtf8(ctx.alloc(), "Hello"), 0, true, ctx.newPromise());
            http2Client.flush(ctx);
        });

        assertTrue(latch.await(DEFAULT_AWAIT_TIMEOUT_SECONDS, SECONDS));
        EmbeddedChannelAssert.assertChannel(targetChannel).hasOutboundMessage().hasSize(3);

        assertThat(targetChannel.outboundMessages().poll())
                .isInstanceOf(Http2FrameWrapper.class)
                .satisfies(wrapper -> assertThat(frame(wrapper)).isInstanceOf(Http2SettingsFrame.class));
        assertThat(targetChannel.outboundMessages().poll())
                .isInstanceOf(Http2FrameWrapper.class)
                .satisfies(wrapper -> assertThat(frame(wrapper)).isInstanceOf(Http2HeadersFrame.class));
        assertThat(targetChannel.outboundMessages().poll())
                .isInstanceOf(Http2FrameWrapper.class)
                .satisfies(wrapper -> {
                    assertThat(frame(wrapper)).isInstanceOf(Http2DataFrame.class);
                    release(wrapper);
                });
    }

    private void bootstrapEnv() throws Exception {
        final CountDownLatch prefaceWrittenLatch = new CountDownLatch(1);
        sb = new ServerBootstrap();
        cb = new Bootstrap();

        final AtomicReference<Http2FrontendHandler> serverHandlerRef = new AtomicReference<>();
        final CountDownLatch serverInitLatch = new CountDownLatch(1);
        sb.group(new DefaultEventLoopGroup());
        sb.channel(LocalServerChannel.class);
        sb.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                serverConnectedChannel = ch;
                ChannelPipeline p = ch.pipeline();

                serverHandlerRef.set(new Http2FrontendHandler(connectionContext));
                p.addLast(serverHandlerRef.get());
                serverInitLatch.countDown();
            }
        });

        cb.group(new DefaultEventLoopGroup());
        cb.channel(LocalChannel.class);
        cb.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                connectionContext
                        .withClientAddr(new Address("localhost", 8080))
                        .withClientChannel(ch);
                p.addLast(new Http2ConnectionHandlerBuilder()
                        .server(false)
                        .frameListener(new Http2FrameAdapter())
                        .validateHeaders(false)
                        .gracefulShutdownTimeoutMillis(0)
                        .build());
                p.addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                        if (evt instanceof Http2ConnectionPrefaceAndSettingsFrameWrittenEvent) {
                            prefaceWrittenLatch.countDown();
                            ctx.pipeline().remove(this);
                        }
                    }
                });
            }
        });

        serverChannel = sb.bind(new LocalAddress("Http2FrontendHandlerTest")).sync().channel();

        ChannelFuture ccf = cb.connect(serverChannel.localAddress());
        assertTrue(ccf.awaitUninterruptibly().isSuccess());
        clientChannel = ccf.channel();
        assertTrue(prefaceWrittenLatch.await(DEFAULT_AWAIT_TIMEOUT_SECONDS, SECONDS));
        http2Client = clientChannel.pipeline().get(Http2ConnectionHandler.class);
        assertTrue(serverInitLatch.await(DEFAULT_AWAIT_TIMEOUT_SECONDS, SECONDS));
    }

    private static Http2Headers dummyHeaders() {
        return new DefaultHttp2Headers(false)
                .method(new AsciiString("GET"))
                .scheme(new AsciiString("https"))
                .authority(new AsciiString("example.org"))
                .path(new AsciiString("/some/path/resource2"));
    }
}
