package com.github.chhsiaoninety.nitmproxy.handler.protocol.tls;

import static io.netty.util.ReferenceCountUtil.safeRelease;
import static java.lang.String.format;

import com.github.chhsiaoninety.nitmproxy.Address;
import com.github.chhsiaoninety.nitmproxy.ConnectionContext;
import com.github.chhsiaoninety.nitmproxy.NitmProxyMaster;
import com.github.chhsiaoninety.nitmproxy.enums.Handler;
import com.github.chhsiaoninety.nitmproxy.tls.TlsUtil;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.AbstractSniHandler;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import javax.net.ssl.SSLException;

public class TlsFrontendHandler extends ChannelDuplexHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(TlsFrontendHandler.class);

  private NitmProxyMaster master;
  private ConnectionContext connectionContext;

  public TlsFrontendHandler(NitmProxyMaster master, ConnectionContext connectionContext) {
    this.master = master;
    this.connectionContext = connectionContext;
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    super.userEventTriggered(ctx, evt);
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    LOGGER.debug("{} : handlerAdded", connectionContext);

    if (!connectionContext.isHttps()) {
      configHttp1(ctx);
    } else {
      ctx.pipeline()
          .addBefore(ctx.name(), null, new SniExtractorHandler())
          .addBefore(ctx.name(), null, new AlpnNegotiateHandler(ctx));
    }
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    LOGGER.debug("{} : handlerRemoved", connectionContext);

    ctx.flush();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    LOGGER.debug("{} : channelInactive", connectionContext);
    if (connectionContext.connected()) {
      connectionContext.serverChannel().close();
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    LOGGER.error(format("%s : exceptionCaught with %s",
        connectionContext, cause.getMessage()),
        cause);
    ctx.close();
  }

  private SslHandler sslHandler(ByteBufAllocator alloc) throws SSLException {
    return TlsUtil.ctxForServer(connectionContext).newHandler(alloc);
  }

  private void configHttp1(ChannelHandlerContext ctx) {
    ctx.pipeline().replace(this, null, connectionContext.handler(Handler.HTTP1_FRONTEND));
  }

  private void configHttp2(ChannelHandlerContext ctx) {
    ctx.pipeline().replace(this, null, connectionContext.handler(Handler.HTTP2_FRONTEND));
  }

  private class AlpnNegotiateHandler extends AbstractAlpnHandler<String> {

    private ChannelHandlerContext tlsCtx;

    public AlpnNegotiateHandler(ChannelHandlerContext tlsCtx) {
      this.tlsCtx = tlsCtx;
    }

    @Override
    protected void onLookupComplete(ChannelHandlerContext ctx, List<String> protocols,
        Future<String> future) throws Exception {
      if (!future.isSuccess()) {
        LOGGER.debug("ALPN negotiate failed with {}", future.cause().getMessage());
        ctx.close();
      } else {
        LOGGER.debug("ALPN negotiated with {}", future.getNow());
        SslHandler sslHandler = sslHandler(ctx.alloc());
        try {
          ctx.pipeline()
              .addAfter(ctx.name(), null, new AlpnHandler(tlsCtx))
              .replace(ctx.name(), null, sslHandler);
          sslHandler = null;
        } finally {
          if (sslHandler != null) {
            safeRelease(sslHandler.engine());
          }
        }
      }
    }

    @Override
    protected Future<String> lookup(ChannelHandlerContext ctx, List<String> protocols)
        throws Exception {
      LOGGER.debug("Client ALPN lookup with {}", protocols);
      connectionContext.tlsCtx().protocolsPromise().setSuccess(protocols);
      return connectionContext.tlsCtx().protocolPromise();
    }
  }

  private class SniExtractorHandler extends AbstractSniHandler<Object> {

    @Override
    protected Future<Object> lookup(ChannelHandlerContext ctx, String hostname) {
      LOGGER.debug("Client SNI lookup with {}", hostname);
      if (hostname != null) {
        connectionContext.withServerAddr(new Address(hostname, connectionContext.getServerAddr().getPort()));
      }
      return ctx.executor().newSucceededFuture(null);
    }

    @Override
    protected void onLookupComplete(ChannelHandlerContext ctx, String hostname, Future<Object> future) throws Exception {
      ctx.pipeline().remove(ctx.name());
    }
  }

  private class AlpnHandler extends ApplicationProtocolNegotiationHandler {

    private ChannelHandlerContext tlsCtx;

    private AlpnHandler(ChannelHandlerContext tlsCtx) {
      super(ApplicationProtocolNames.HTTP_1_1);
      this.tlsCtx = tlsCtx;
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
      if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
        configHttp1(tlsCtx);
      } else if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
        configHttp2(tlsCtx);
      } else {
        throw new IllegalStateException("unknown protocol: " + protocol);
      }
    }
  }
}
