package com.github.chhsiao90.nitmproxy;

import com.google.common.base.Joiner;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.net.HttpHeaders.*;
import static io.netty.buffer.Unpooled.*;
import static java.nio.charset.StandardCharsets.*;

public class HttpObjectUtil {
    private HttpObjectUtil() {
    }

    public static DefaultFullHttpRequest fullRequest() {
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        request.headers().add(HttpHeaderNames.HOST, "localhost");
        return request;
    }

    public static ByteBuf fullRequestAsBytes() {
        return fullRequestAsBytes(fullRequest());
    }

    public static ByteBuf fullRequestAsBytes(FullHttpRequest request) {
        String req = String.format("%s %s %s\r\n%s\r\n",
                request.method(),
                request.uri(),
                request.protocolVersion(),
                headersString(request.headers()));
        return Unpooled.buffer().writeBytes(req.getBytes());
    }

    public static DefaultFullHttpResponse fullResponse(String content) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                copiedBuffer(content, UTF_8));
        response.headers()
                .add(CONTENT_TYPE, "text/plain")
                .add(CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    public static DefaultHttpResponse response() {
        DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().add(CONTENT_TYPE, "text/plain");
        return response;
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
