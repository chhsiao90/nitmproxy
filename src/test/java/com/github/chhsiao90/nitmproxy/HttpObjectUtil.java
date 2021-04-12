package com.github.chhsiao90.nitmproxy;

import com.google.common.base.Joiner;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.List;
import java.util.stream.Collectors;

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
        return requestBytes(request());
    }

    public static ByteBuf requestBytes(HttpRequest request) {
        String req = String.format("%s %s %s\r\n%s\r\n",
                                   request.method(),
                                   request.uri(),
                                   request.protocolVersion(),
                                   headersString(request.headers()));
        return Unpooled.buffer().writeBytes(req.getBytes());
    }

    private static String headersString(HttpHeaders headers) {
        if (headers.isEmpty()) {
            return "";
        }

        List<String> headerStrings = headers.entries().stream()
                                            .map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue()))
                                            .collect(Collectors.toList());
        return Joiner.on("\r\n").join(headerStrings) + "\r\n";
    }

    public static ByteBuf responseBytes() {
        return Unpooled.buffer().writeBytes(
                "HTTP/1.1 200 OK\r\n\r\n".getBytes());
    }

    public static ByteBuf responseBytes(HttpResponse response) {
        String resp = String.format("%s %s %s\r\n%s\r\n",
                                   response.protocolVersion(),
                                   response.status().codeAsText(),
                                   response.status().reasonPhrase(),
                                   headersString(response.headers()));
        return Unpooled.buffer().writeBytes(resp.getBytes());
    }
}
