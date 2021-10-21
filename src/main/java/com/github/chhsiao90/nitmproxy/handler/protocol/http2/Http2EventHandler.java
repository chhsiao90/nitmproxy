package com.github.chhsiao90.nitmproxy.handler.protocol.http2;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import com.github.chhsiao90.nitmproxy.http.HttpUtil;
import com.github.chhsiao90.nitmproxy.listener.NitmProxyListener;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.PromiseCombiner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.chhsiao90.nitmproxy.http.HttpHeadersUtil.*;
import static io.netty.util.ReferenceCountUtil.*;
import static java.lang.System.*;

public class Http2EventHandler extends ChannelDuplexHandler {

    private NitmProxyListener listener;
    private ConnectionContext connectionContext;

    private Map<Integer, FrameCollector> streams = new ConcurrentHashMap<>();

    /**
     * Create new instance of http1 event handler.
     *
     * @param connectionContext the connection context
     */
    public Http2EventHandler(ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
        this.listener = connectionContext.listener();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
            throws Exception {
        if (!(msg instanceof Http2FrameWrapper)) {
            ctx.write(msg, promise);
            return;
        }

        Http2FrameWrapper<?> frameWrapper = (Http2FrameWrapper<?>) msg;
        FrameCollector frameCollector = streams.computeIfAbsent(frameWrapper.streamId(), this::newFrameCollector);
        List<Http2FrameWrapper<?>> output = listener.onHttp2Response(connectionContext, frameWrapper);
        boolean streamEnded = output.stream()
                .map(wrapper -> frameCollector.onResponseFrame(wrapper.frame()))
                .findFirst()
                .orElse(false);
        writeFrames(ctx, output, promise);
        if (streamEnded) {
            try {
                frameCollector.collect().ifPresent(listener::onHttpEvent);
            } finally {
                frameCollector.release();
                streams.remove(frameWrapper.streamId());
            }
        }
    }

    private void writeFrames(ChannelHandlerContext ctx, List<? extends Http2FrameWrapper<?>> frames,
            ChannelPromise promise) {
        if (frames.isEmpty()) {
            promise.setSuccess();
        } else if (frames.size() == 1) {
            ctx.write(frames.get(0), promise);
        } else {
            PromiseCombiner combiner = new PromiseCombiner(ctx.executor());
            frames.stream().map(ctx::write).forEach(combiner::add);
            combiner.finish(promise);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof Http2FrameWrapper)
            || (!Http2FrameWrapper.isFrame(msg, Http2HeadersFrame.class)
                && !Http2FrameWrapper.isFrame(msg, Http2DataFrame.class))) {
            ctx.fireChannelRead(msg);
            return;
        }

        Http2FrameWrapper<?> frameWrapper = (Http2FrameWrapper<?>) msg;
        FrameCollector frameCollector = streams.computeIfAbsent(frameWrapper.streamId(), this::newFrameCollector);
        Optional<Http2FramesWrapper> requestOptional = frameCollector.onRequestFrame(frameWrapper.frame());
        if (!requestOptional.isPresent()) {
            return;
        }

        Http2FramesWrapper request = requestOptional.get();
        Optional<Http2FramesWrapper> responseOptional = listener.onHttp2Request(connectionContext, request);
        if (!responseOptional.isPresent()) {
            request.getAllFrames().forEach(ctx::fireChannelRead);
            return;
        }

        try {
            Http2FramesWrapper response = responseOptional.get();
            frameCollector.onResponseHeadersFrame(response.getHeaders());
            response.getData().forEach(frameCollector::onResponseDataFrame);
            frameCollector.collect().ifPresent(listener::onHttpEvent);
            response.getAllFrames().forEach(ctx::write);
            ctx.flush();
        } finally {
            release(request);
            frameCollector.release();
            streams.remove(frameWrapper.streamId());
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        streams.values().forEach(FrameCollector::release);
    }

    private FrameCollector newFrameCollector(int streamId) {
        return new FrameCollector(streamId, HttpEvent.builder(connectionContext));
    }

    private static class FrameCollector {

        private int streamId;
        private HttpEvent.Builder httpEventBuilder;
        private Http2HeadersFrame requestHeader;
        private List<Http2DataFrame> requestData = new ArrayList<>();
        private boolean requestDone;

        public FrameCollector(int streamId, HttpEvent.Builder httpEventBuilder) {
            this.streamId = streamId;
            this.httpEventBuilder = httpEventBuilder;
        }

        /**
         * Handles a http2 frame of the request, and return full request frames while the request was ended.
         *
         * @param frame a http2 frame
         * @return full request frames if the request was ended, return empty if there are more frames of the request
         */
        public Optional<Http2FramesWrapper> onRequestFrame(Http2Frame frame) {
            if (frame instanceof Http2HeadersFrame) {
                requestHeader = (Http2HeadersFrame) frame;
                Http2Headers headers = requestHeader.headers();
                httpEventBuilder.method(HttpMethod.valueOf(headers.method().toString()))
                                .version(HttpUtil.HTTP_2)
                                .host(headers.authority().toString())
                                .path(headers.path().toString())
                                .requestTime(currentTimeMillis());
                requestDone = requestHeader.isEndStream();
            } else if (frame instanceof Http2DataFrame) {
                Http2DataFrame data = (Http2DataFrame) frame;
                requestData.add(data);
                httpEventBuilder.addRequestBodySize(data.content().readableBytes());
                requestDone = data.isEndStream();
            }

            if (requestDone) {
                Http2FramesWrapper request = Http2FramesWrapper
                        .builder(streamId)
                        .headers(requestHeader)
                        .data(requestData)
                        .build();
                requestData.clear();
                return Optional.of(request);
            }
            return Optional.empty();
        }

        public boolean onResponseFrame(Http2Frame frame) {
            if (frame instanceof Http2HeadersFrame) {
                return onResponseHeadersFrame((Http2HeadersFrame) frame);
            }
            if (frame instanceof Http2DataFrame) {
                return onResponseDataFrame((Http2DataFrame) frame);
            }
            return false;
        }

        public boolean onResponseHeadersFrame(Http2HeadersFrame frame) {
            Http2Headers headers = frame.headers();
            httpEventBuilder.status(getStatus(headers))
                            .contentType(getContentType(headers))
                            .responseTime(currentTimeMillis());
            return frame.isEndStream();
        }

        public boolean onResponseDataFrame(Http2DataFrame frame) {
            httpEventBuilder.addResponseBodySize(frame.content().readableBytes());
            return frame.isEndStream();
        }

        public Optional<HttpEvent> collect() {
            if (requestDone) {
                return Optional.of(httpEventBuilder.build());
            }
            return Optional.empty();
        }

        public void release() {
            requestData.forEach(ReferenceCountUtil::release);
        }
    }
}
