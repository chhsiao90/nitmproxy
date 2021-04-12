package com.github.chhsiao90.nitmproxy.handler.protocol.http2;

import static io.netty.buffer.ByteBufUtil.writeUtf8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.chhsiao90.nitmproxy.NitmProxyConfig;
import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;

import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2ConnectionPrefaceAndSettingsFrameWrittenEvent;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.AsciiString;
import io.netty.util.concurrent.Future;

public class Http2BackendHandlerTest {

  private static final long DEFAULT_AWAIT_TIMEOUT_SECONDS = 15;

  private ConnectionContext connectionContext;
  private Http2FrameListener serverListener;

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
    connectionContext = new ConnectionContext(master);

    serverListener = mock(Http2FrameListener.class);
    targetChannel = new EmbeddedChannel();
  }

  @After
  public void tearDown() {
    targetChannel.finishAndReleaseAll();
    Channel serverConnectedChannel = this.serverConnectedChannel;
    if (serverConnectedChannel != null) {
      serverConnectedChannel.close().syncUninterruptibly();
      this.serverConnectedChannel = null;
    }
    if (clientChannel != null) {
      clientChannel.close().syncUninterruptibly();
      clientChannel = null;
    }
    if (serverChannel != null) {
      serverChannel.close().syncUninterruptibly();
      serverChannel = null;
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
    doAnswer(mock -> {
      latch.countDown();
      return null;
    }).when(serverListener).onHeadersRead(any(), anyInt(), any(), anyInt(),
        anyShort(),anyBoolean(), anyInt(), anyBoolean());

    Http2Headers headers = dummyHeaders();
    Http2TestUtil.runInChannel(clientChannel, () -> clientChannel.writeAndFlush(
        new Http2FrameWrapper<>(1, new DefaultHttp2HeadersFrame(
            dummyHeaders(), true))));

    assertTrue(latch.await(DEFAULT_AWAIT_TIMEOUT_SECONDS, SECONDS));
    verify(serverListener).onHeadersRead(any(ChannelHandlerContext.class), eq(1), eq(headers),
        eq(0), eq((short) 16), eq(false), eq(0), eq(true));
  }

  @Test
  public void shouldSendHeadersWithData() throws Exception {
    bootstrapEnv();
    CountDownLatch latch = new CountDownLatch(1);
    doAnswer(mock -> {
      latch.countDown();
      ByteBuf buf = (ByteBuf) mock.getArguments()[2];
      int padding = (Integer) mock.getArguments()[3];
      return buf.readableBytes() + padding;
    }).when(serverListener).onDataRead(any(), anyInt(), any(), anyInt(), anyBoolean());

    Http2Headers headers = dummyHeaders();
    ChannelHandlerContext ctx = clientChannel.pipeline().firstContext();
    Http2TestUtil.runInChannel(clientChannel, () -> {
      clientChannel.write(new Http2FrameWrapper<>(1, new DefaultHttp2HeadersFrame(
          headers, false)));
      clientChannel.write(new Http2DataFrameWrapper(1, new DefaultHttp2DataFrame(
          writeUtf8(ctx.alloc(), "Hello"), true)));
      clientChannel.flush();
    });

    assertTrue(latch.await(DEFAULT_AWAIT_TIMEOUT_SECONDS, SECONDS));
    verify(serverListener).onHeadersRead(any(ChannelHandlerContext.class), eq(1), eq(headers),
        eq(0), eq((short) 16), eq(false), eq(0), eq(false));
    verify(serverListener).onDataRead(any(ChannelHandlerContext.class),
        eq(1), any(ByteBuf.class), eq(0), eq(true));
  }

  @Test
  public void shouldMappingStreamIds() throws Exception {
    bootstrapEnv();
    CountDownLatch latch = new CountDownLatch(2);
    doAnswer(mock -> {
      latch.countDown();
      ByteBuf buf = (ByteBuf) mock.getArguments()[2];
      int padding = (Integer) mock.getArguments()[3];
      return buf.readableBytes() + padding;
    }).when(serverListener).onDataRead(any(), anyInt(), any(), anyInt(), anyBoolean());

    Http2Headers headers = dummyHeaders();
    ChannelHandlerContext ctx = clientChannel.pipeline().firstContext();
    Http2TestUtil.runInChannel(clientChannel, () -> {
      clientChannel.write(new Http2FrameWrapper<>(3, new DefaultHttp2HeadersFrame(
          headers, false)));
      clientChannel.write(new Http2DataFrameWrapper(3, new DefaultHttp2DataFrame(
          writeUtf8(ctx.alloc(), "Hello"), true)));
      clientChannel.write(new Http2FrameWrapper<>(5, new DefaultHttp2HeadersFrame(
          headers, false)));
      clientChannel.write(new Http2DataFrameWrapper(5, new DefaultHttp2DataFrame(
          writeUtf8(ctx.alloc(), "Hello"), true)));
      clientChannel.flush();
    });

    assertTrue(latch.await(DEFAULT_AWAIT_TIMEOUT_SECONDS, SECONDS));
    InOrder inOrder = inOrder(serverListener);
    inOrder.verify(serverListener).onHeadersRead(any(ChannelHandlerContext.class), eq(1), eq(headers),
        eq(0), eq((short) 16), eq(false), eq(0), eq(false));
    inOrder.verify(serverListener).onDataRead(any(ChannelHandlerContext.class),
        eq(1), any(ByteBuf.class), eq(0), eq(true));
    inOrder.verify(serverListener).onHeadersRead(any(ChannelHandlerContext.class), eq(3), eq(headers),
        eq(0), eq((short) 16), eq(false), eq(0), eq(false));
    inOrder.verify(serverListener).onDataRead(any(ChannelHandlerContext.class),
        eq(3), any(ByteBuf.class), eq(0), eq(true));
  }

  private void bootstrapEnv() throws Exception {
    final CountDownLatch prefaceWrittenLatch = new CountDownLatch(1);
    sb = new ServerBootstrap();
    cb = new Bootstrap();

    final CountDownLatch serverInitLatch = new CountDownLatch(1);
    sb.group(new DefaultEventLoopGroup());
    sb.channel(LocalServerChannel.class);
    sb.childHandler(new ChannelInitializer<Channel>() {
      @Override
      protected void initChannel(Channel ch) throws Exception {
        serverConnectedChannel = ch;
        ChannelPipeline p = ch.pipeline();
        p.addLast(new Http2ConnectionHandlerBuilder()
            .server(true)
            .frameListener(serverListener)
            .validateHeaders(false)
            .gracefulShutdownTimeoutMillis(0)
            .build());
        serverInitLatch.countDown();
      }
    });

    cb.group(new DefaultEventLoopGroup());
    cb.channel(LocalChannel.class);
    cb.handler(new ChannelInitializer<Channel>() {
      @Override
      protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new Http2BackendHandler(connectionContext));
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

    serverChannel = sb.bind(new LocalAddress("Http2BackendHandlerTest")).sync().channel();

    ChannelFuture ccf = cb.connect(serverChannel.localAddress());
    assertTrue(ccf.awaitUninterruptibly().isSuccess());
    clientChannel = ccf.channel();
    assertTrue(prefaceWrittenLatch.await(DEFAULT_AWAIT_TIMEOUT_SECONDS, SECONDS));
    assertTrue(serverInitLatch.await(DEFAULT_AWAIT_TIMEOUT_SECONDS, SECONDS));

    connectionContext
        .withClientAddr(new Address("localhost", 8080))
        .withClientChannel(clientChannel)
        .withServerAddr(new Address("localhost", 8080))
        .withServerChannel(targetChannel);
  }

  private static Http2Headers dummyHeaders() {
    return new DefaultHttp2Headers(false)
        .method(new AsciiString("GET"))
        .scheme(new AsciiString("https"))
        .authority(new AsciiString("example.org"))
        .path(new AsciiString("/some/path/resource2"));
  }
}
