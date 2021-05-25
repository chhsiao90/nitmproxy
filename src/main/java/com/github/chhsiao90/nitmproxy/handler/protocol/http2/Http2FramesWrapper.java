package com.github.chhsiao90.nitmproxy.handler.protocol.http2;

import com.github.chhsiao90.nitmproxy.exception.NitmProxyException;
import com.google.common.collect.ImmutableList;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;

import java.util.ArrayList;
import java.util.List;

import static com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FrameWrapper.*;
import static com.google.common.net.HttpHeaders.*;
import static java.util.stream.Collectors.*;

public class Http2FramesWrapper {

    private int streamId;
    private Http2HeadersFrame headersFrame;
    private List<Http2DataFrame> dataFrames;

    public Http2FramesWrapper(Builder builder) {
        this.streamId = builder.streamId;
        this.headersFrame = builder.headersFrame;
        this.dataFrames = builder.dataFrames;
    }

    public static Builder builder(int streamId) {
        return new Builder(streamId);
    }

    public int getStreamId() {
        return streamId;
    }

    public Http2HeadersFrame getHeaders() {
        return headersFrame;
    }

    public List<Http2DataFrame> getData() {
        return dataFrames;
    }

    public List<Http2FrameWrapper<?>> getAllFrames() {
        return ImmutableList.<Http2FrameWrapper<?>>builder()
                .add(frameWrapper(streamId, headersFrame))
                .addAll(dataFrames.stream().map(frame -> frameWrapper(streamId, frame)).collect(toList()))
                .build();
    }

    public static class Builder {
        private int streamId;
        private Http2HeadersFrame headersFrame;
        private List<Http2DataFrame> dataFrames = new ArrayList<>();

        private Builder(int streamId) {
            this.streamId = streamId;
        }

        public Builder request(FullHttpRequest request) {
            headersFrame = new DefaultHttp2HeadersFrame(new DefaultHttp2Headers(),
                    request.content().readableBytes() == 0);
            headersFrame.headers()
                        .authority(request.headers().get(HOST))
                        .path(request.uri())
                        .method(request.method().name())
                        .scheme("https");
            request.headers().forEach(entry -> headersFrame
                    .headers().add(entry.getKey().toLowerCase(), entry.getValue()));
            if (request.content().readableBytes() > 0) {
                dataFrames.add(new DefaultHttp2DataFrame(request.content(), true));
            }
            return this;
        }

        public Builder response(FullHttpResponse response) {
            headersFrame = new DefaultHttp2HeadersFrame(new DefaultHttp2Headers(),
                    response.content().readableBytes() == 0);
            headersFrame.headers().status(response.status().codeAsText());
            response.headers().forEach(entry -> headersFrame
                    .headers().add(entry.getKey().toLowerCase(), entry.getValue()));
            if (response.content().readableBytes() > 0) {
                dataFrames.add(new DefaultHttp2DataFrame(response.content(), true));
            }
            return this;
        }

        public Builder headers(Http2HeadersFrame headersFrame) {
            this.headersFrame = headersFrame;
            return this;
        }

        public Builder data(Http2DataFrame dataFrame) {
            dataFrames.add(dataFrame);
            return this;
        }

        public Builder data(List<Http2DataFrame> dataFrames) {
            this.dataFrames.addAll(dataFrames);
            return this;
        }

        public Http2FramesWrapper build() {
            if (headersFrame == null) {
                throw new NitmProxyException("null headers");
            }
            boolean ended = headersFrame.isEndStream();
            for (Http2DataFrame data : dataFrames) {
                if (ended) {
                    throw new NitmProxyException("stream was ended, but found another data frame");
                }
                ended = data.isEndStream();
            }
            if (!ended) {
                throw new NitmProxyException("stream not ended");
            }
            return new Http2FramesWrapper(this);
        }
    }
}
