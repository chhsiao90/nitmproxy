package com.github.chhsiaoninety.nitm.nitmproxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class HttpObjectUtil {
    private HttpObjectUtil() {
    }

    public static DefaultFullHttpRequest request() {
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        request.headers().add(HttpHeaderNames.HOST, "localhost");
        return request;
    }

    public static DefaultFullHttpResponse response() {
        return new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    }

    public static ByteBuf requestBytes() {
        return Unpooled.buffer().writeBytes(
                "GET / HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes());
    }

    public static ByteBuf responseBytes() {
        return Unpooled.buffer().writeBytes(
                "HTTP/1.1 200 OK\r\n\r\n".getBytes());
    }
}
