package com.github.chhsiao90.nitmproxy.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AsciiString;

import static com.google.common.net.HttpHeaders.*;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static java.lang.String.*;
import static java.nio.charset.StandardCharsets.*;

public class HttpUtil {

    public static final int HTTP_PORT = 80;
    public static final int HTTPS_PORT = 443;
    public static final HttpVersion HTTP_2 = new HttpVersion("http/2.0", true);

    private HttpUtil() {
    }

    /**
     * Create a http request.
     *
     * @param version the http version
     * @param method  the http method
     * @param host    the host
     * @param url     the url
     * @return the http request
     */
    public static HttpRequest request(HttpVersion version, HttpMethod method, String host,
            String url) {
        FullHttpRequest request = new DefaultFullHttpRequest(version, method, url);
        request.headers().set(HOST, host);
        return request;
    }

    /**
     * Encode the request into a buffer.
     *
     * @param buffer  the buffer
     * @param request the request
     */
    public static void write(ByteBuf buffer, HttpRequest request) {
        buffer.writeCharSequence(
                format("%s %s %s\r\n", request.method(), request.uri(), request.protocolVersion()),
                UTF_8);
        write(buffer, request.headers());
        buffer.writeCharSequence("\r\n", US_ASCII);
    }

    /**
     * Encode the headers into a buffer.
     *
     * @param buffer  the buffer
     * @param headers the headers
     */
    public static void write(ByteBuf buffer, HttpHeaders headers) {
        if (headers.isEmpty()) {
            return;
        }
        headers.entries().stream()
               .map(entry -> format("%s: %s\r\n", entry.getKey(), entry.getValue()))
               .forEach(line -> buffer.writeCharSequence(line, UTF_8));
    }

    /**
     * Create a json request.
     *
     * @param version the http version
     * @param method  the http method
     * @param host    the host
     * @param url     the url
     * @return the http request
     */
    public static HttpRequest jsonRequest(HttpVersion version, HttpMethod method, String host,
            String url, String json) {
        FullHttpRequest request = new DefaultFullHttpRequest(version, method, url);
        byte[] bodyBytes = json.getBytes(UTF_8);
        request.headers()
               .set(HOST, host)
               .set(CONTENT_LENGTH, bodyBytes.length)
               .set(CONTENT_TYPE, APPLICATION_JSON);
        request.content().writeBytes(bodyBytes);
        return request;
    }

    /**
     * Create a json response.
     *
     * @param ctx     the channel handler context
     * @param version the http version
     * @param json    the json
     * @return the response
     */
    public static FullHttpResponse jsonResponse(
            ChannelHandlerContext ctx, HttpVersion version, String json) {
        return response(ctx.alloc(), OK, version, APPLICATION_JSON, json);
    }

    /**
     * Create a text response.
     *
     * @param alloc   the allocator
     * @param version the http version
     * @param text    the text
     * @return the response
     */
    public static FullHttpResponse textResponse(
            ByteBufAllocator alloc, HttpVersion version, String text) {
        return response(alloc, OK, version, TEXT_PLAIN, text);
    }

    /**
     * Create a json response.
     *
     * @param alloc   the allocator
     * @param version the http version
     * @param status  the status
     * @return the response
     */
    public static FullHttpResponse errorResponse(
            ByteBufAllocator alloc, HttpVersion version, HttpResponseStatus status) {
        return response(alloc, status, version, TEXT_PLAIN, "");
    }

    /**
     * Create a response.
     *
     * @param alloc       the allocator
     * @param status      the status
     * @param version     the http version
     * @param contentType the content type
     * @param body        the body
     * @return the response
     */
    public static FullHttpResponse response(ByteBufAllocator alloc, HttpResponseStatus status,
            HttpVersion version,
            AsciiString contentType, String body) {
        FullHttpResponse response = new DefaultFullHttpResponse(version, status, alloc.buffer());
        byte[] bodyBytes = body.getBytes(UTF_8);
        response.headers()
                .set(CONTENT_TYPE, contentType)
                .set(CONTENT_LENGTH, bodyBytes.length);
        response.content().writeBytes(bodyBytes);
        return response;
    }
}
