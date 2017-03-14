package com.github.chhsiao.nitm.nitmproxy.layer.protocol.http2;

import com.github.chhsiao.nitm.nitmproxy.ConnectionInfo;
import com.github.chhsiao.nitm.nitmproxy.NitmProxyConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.handler.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Pattern;

public class Http2FrontendHandler extends ChannelInboundHandlerAdapter {
    private static final Pattern PATH_PATTERN = Pattern.compile("(https?)://([a-zA-Z0-9\\.\\-]+)(:(\\d+))?(/.*)");
    private static final Pattern TUNNEL_ADDR_PATTERN = Pattern.compile("^([a-zA-Z0-9\\.\\-_]+):(\\d+)");

    private static final Logger LOGGER = LoggerFactory.getLogger(Http2FrontendHandler.class);

    private NitmProxyConfig config;
    private ConnectionInfo connectionInfo;
    private Channel outboundChannel;

    public Http2FrontendHandler(NitmProxyConfig config, ConnectionInfo connectionInfo, Channel outboundChannel) {
        this.config = config;
        this.connectionInfo = connectionInfo;
        this.outboundChannel = outboundChannel;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : handlerAdded", connectionInfo);

        Http2Connection connection = new DefaultHttp2Connection(true);
        ChannelHandler http2ConnHandler = new HttpToHttp2ConnectionHandlerBuilder()
                .frameListener(new DelegatingDecompressorFrameListener(
                        connection,
                        new InboundHttp2ToHttpAdapterBuilder(connection)
                                .maxContentLength(config.getMaxContentLength())
                                .propagateSettings(true)
                                .build()))
                .connection(connection)
                .frameLogger(new Http2FrameLogger(LogLevel.DEBUG))
                .build();
        ctx.pipeline()
           .addBefore(ctx.name(), null, http2ConnHandler)
           .addBefore(ctx.name(), null, new Http2Handler());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} : channelInactive", connectionInfo);
        outboundChannel.close();
    }

    private class Http2Handler extends ChannelDuplexHandler {
        private Deque<String> streams = new ConcurrentLinkedDeque<>();

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            LOGGER.info("[Client ({})] => [Server ({})] : {}",
                        connectionInfo.getClientAddr(), connectionInfo.getServerAddr(),
                        msg);

            if (msg instanceof HttpRequest) {
                String streamId = ((HttpRequest) msg).headers().get(
                        HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
                if (streamId == null) {
                    throw new IllegalStateException("No streamId");
                }
                streams.offer(streamId);
            }

            outboundChannel.writeAndFlush(msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            LOGGER.info("[Client ({})] <= [Server ({})] : {}",
                        connectionInfo.getClientAddr(), connectionInfo.getServerAddr(),
                        msg);

            if (msg instanceof HttpResponse) {
                HttpResponse response = (HttpResponse) msg;
                if (!response.headers().contains(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text())) {
                    if (streams.isEmpty()) {
                        throw new IllegalStateException("No active streams");
                    }
                    response.headers().add(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(),
                                           streams.poll());
                }
            }

            ctx.write(msg, promise);
        }
    }
}
