package com.github.chhsiao90.nitmproxy.handler.protocol.http2;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.NitmProxyMaster;
import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.http.HttpUtil;
import com.github.chhsiao90.nitmproxy.listener.HttpEventListener;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.chhsiao90.nitmproxy.http.HttpHeadersUtil.*;
import static java.lang.System.*;

public class Http2EventHandler extends ChannelDuplexHandler {

    private HttpEventListener listener;
    private ConnectionContext connectionContext;

    private Map<Integer, FrameCollector> streams = new ConcurrentHashMap<>();

    /**
     * Create new instance of http1 event handler.
     *
     * @param master            the master
     * @param connectionContext the connection context
     */
    public Http2EventHandler(
            NitmProxyMaster master,
            ConnectionContext connectionContext) {
        this.listener = master.httpEventListener();
        this.connectionContext = connectionContext;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
            throws Exception {
        if (msg instanceof Http2FrameWrapper) {
            Http2FrameWrapper<?> frameWrapper = (Http2FrameWrapper<?>) msg;
            FrameCollector frameCollector = streams.computeIfAbsent(
                    frameWrapper.streamId(), ignored -> new FrameCollector());
            if (frameCollector.onResponseFrame(frameWrapper.frame())) {
                HttpEvent event = frameCollector.collect();
                try {
                    if (event != null) {
                        listener.onHttpEvent(event);
                    }
                } finally {
                    streams.remove(frameWrapper.streamId());
                }
            }
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2FrameWrapper) {
            Http2FrameWrapper<?> frameWrapper = (Http2FrameWrapper<?>) msg;
            FrameCollector frameCollector = streams.computeIfAbsent(
                    frameWrapper.streamId(), ignored -> new FrameCollector());
            frameCollector.onRequestFrame(frameWrapper.frame());
        }
        super.channelRead(ctx, msg);
    }

    private class FrameCollector {

        private HttpEvent.Builder httpEventBuilder;
        private boolean requestDone;

        public FrameCollector() {
            httpEventBuilder = HttpEvent.builder(connectionContext);
        }

        public void onRequestFrame(Http2Frame frame) {
            if (frame instanceof Http2HeadersFrame) {
                Http2HeadersFrame headersFrame = (Http2HeadersFrame) frame;
                Http2Headers headers = headersFrame.headers();
                httpEventBuilder.method(HttpMethod.valueOf(headers.method().toString()))
                                .version(HttpUtil.HTTP_2)
                                .host(headers.authority().toString())
                                .path(headers.path().toString())
                                .requestTime(currentTimeMillis());
                requestDone = headersFrame.isEndStream();
            } else if (frame instanceof Http2DataFrame) {
                Http2DataFrame data = (Http2DataFrame) frame;
                httpEventBuilder.addRequestBodySize(data.content().readableBytes());
                requestDone = data.isEndStream();
            }
        }

        public boolean onResponseFrame(Http2Frame frame) {
            if (frame instanceof Http2HeadersFrame) {
                Http2HeadersFrame headersFrame = (Http2HeadersFrame) frame;
                Http2Headers headers = headersFrame.headers();
                httpEventBuilder.status(getStatus(headers))
                                .contentType(getContentType(headers))
                                .responseTime(currentTimeMillis());
                return headersFrame.isEndStream();
            } else if (frame instanceof Http2DataFrame) {
                Http2DataFrame data = (Http2DataFrame) frame;
                httpEventBuilder.addResponseBodySize(data.content().readableBytes());
                return data.isEndStream();
            }
            return false;
        }

        public HttpEvent collect() {
            if (requestDone) {
                return httpEventBuilder.build();
            }
            return null;
        }
    }
}
