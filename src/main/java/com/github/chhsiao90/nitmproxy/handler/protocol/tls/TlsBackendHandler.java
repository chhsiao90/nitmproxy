package com.github.chhsiao90.nitmproxy.handler.protocol.tls;

import static java.lang.String.format;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.enums.Handler;
import com.github.chhsiao90.nitmproxy.tls.TlsUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.ReferenceCountUtil;
import javax.net.ssl.SSLException;

public class TlsBackendHandler extends ChannelDuplexHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(TlsBackendHandler.class);

  private NitmProxyMaster master;
  private ConnectionContext connectionContext;

  private final List<Object> pendings = new ArrayList<>();

  public TlsBackendHandler(NitmProxyMaster master, ConnectionContext connectionContext) {
    this.master = master;
    this.connectionContext = connectionContext;
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    LOGGER.debug("{} : handlerAdded", connectionContext);

    connectionContext.tlsCtx().protocolsPromise().addListener(future -> {
      if (future.isSuccess()) {
        if (!connectionContext.tlsCtx().isEnabled()) {
          configHttp1(ctx);
        } else {
          configSsl(ctx);
        }
      } else {
        ctx.close();
      }
    });
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    LOGGER.debug("{} : handlerRemoved", connectionContext);

    flushPendings(ctx);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    synchronized (pendings) {
      pendings.add(msg);
    }
    if (ctx.isRemoved()) {
      flushPendings(ctx);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    LOGGER.debug("{} : channelInactive", connectionContext);
    connectionContext.clientChannel().close();
    synchronized (pendings) {
      pendings.forEach(ReferenceCountUtil::release);
    }
    ctx.fireChannelInactive();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    LOGGER.error(format("%s : exceptionCaught with %s",
        connectionContext, cause.getMessage()),
        cause);
    ctx.close();
  }

  private SslHandler sslHandler(ByteBufAllocator alloc) throws SSLException {
    return TlsUtil.ctxForClient(connectionContext)
        .newHandler(alloc, connectionContext.getServerAddr().getHost(),
            connectionContext.getServerAddr().getPort());
  }

  private void flushPendings(ChannelHandlerContext ctx) {
    synchronized (pendings) {
      Iterator<Object> iterator = pendings.iterator();
      while (iterator.hasNext()) {
        ctx.write(iterator.next());
        iterator.remove();
      }
      ctx.flush();
    }
  }

  private void configHttp1(ChannelHandlerContext ctx) {
    ctx.pipeline().replace(this, null, connectionContext.handler(Handler.HTTP1_BACKEND));
  }

  private void configHttp2(ChannelHandlerContext ctx) {
    ctx.pipeline().replace(this, null, connectionContext.handler(Handler.HTTP2_BACKEND));
  }

  /**
   * Configure for ssl.
   *
   * @param ctx the channel handler context
   * @throws SSLException if ssl failure
   */
  public void configSsl(ChannelHandlerContext ctx) throws SSLException {
    SslHandler sslHandler = sslHandler(ctx.alloc());
    ctx.pipeline()
        .addBefore(ctx.name(), null, sslHandler)
        .addBefore(ctx.name(), null, new AlpnHandler(ctx));
  }

  private class AlpnHandler extends ApplicationProtocolNegotiationHandler {
    private ChannelHandlerContext tlsCtx;

    private AlpnHandler(ChannelHandlerContext tlsCtx) {
      super(ApplicationProtocolNames.HTTP_1_1);
      this.tlsCtx = tlsCtx;
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
      if (!connectionContext.tlsCtx().isNegotiated()) {
        connectionContext.tlsCtx().protocolPromise().setSuccess(protocol);
      }
      if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
        configHttp1(tlsCtx);
      } else if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
        configHttp2(tlsCtx);
      } else {
        throw new IllegalStateException("unknown protocol: " + protocol);
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      super.exceptionCaught(ctx, cause);
      if (!connectionContext.tlsCtx().isNegotiated()) {
        connectionContext.tlsCtx().protocolPromise().setFailure(cause);
      }
    }
  }
}
