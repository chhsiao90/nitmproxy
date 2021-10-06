package com.github.chhsiao90.nitmproxy.http;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.Http2Headers;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static java.lang.Integer.*;

public class HttpHeadersUtil {

    private HttpHeadersUtil() {
    }

    /**
     * Get content type from http headers.
     *
     * @param headers the http headers
     * @return the content type
     */
    public static String getContentType(HttpHeaders headers) {
        if (headers.contains(CONTENT_TYPE)) {
            return headers.get(CONTENT_TYPE).split(";")[0];
        }
        return null;
    }

    /**
     * Get content type from http2 headers.
     *
     * @param headers the http2 headers
     * @return the content type
     */
    public static String getContentType(Http2Headers headers) {
        if (headers.contains(CONTENT_TYPE)) {
            return headers.get(CONTENT_TYPE).toString().split(";")[0];
        }
        return null;
    }

    /**
     * Get status from https headers.
     *
     * @param headers the http2 headers
     * @return the status
     */
    public static HttpResponseStatus getStatus(Http2Headers headers) {
        return HttpResponseStatus.valueOf(parseInt(headers.status().toString()));
    }

    /**
     * Get whether is a web socket upgrade or not.
     *
     * @param headers the http headers
     * @return {@code true} if it's a ws upgrade, otherwise, {@code false}
     */
    public static boolean isWebSocketUpgrade(HttpHeaders headers) {
        return headers.contains(HttpHeaderNames.UPGRADE) &&
               headers.containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE, true) &&
               headers.contains(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET, true);
    }
}
