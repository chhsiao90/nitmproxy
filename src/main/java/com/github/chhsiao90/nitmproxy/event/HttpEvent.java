package com.github.chhsiao90.nitmproxy.event;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class HttpEvent {
    private Address client;
    private Address server;

    // request
    private HttpMethod method;
    private HttpVersion version;
    private String host;
    private String path;
    private long requestBodySize;
    private long requestTime;

    // response
    private HttpResponseStatus status;
    private String contentType;
    private long responseBodySize;
    private long responseTime;

    private long timeSpent;

    private HttpEvent(Builder builder) {
        client = builder.client;
        server = builder.server;

        method = builder.method;
        version = builder.version;
        host = builder.host;
        path = builder.path;
        requestBodySize = builder.requestBodySize;
        requestTime = builder.requestTime;

        status = builder.status;
        contentType = builder.contentType;
        responseBodySize = builder.responseBodySize;
        responseTime = builder.responseTime;

        timeSpent = builder.responseTime - builder.requestTime;
    }

    public static Builder builder(ConnectionContext ctx) {
        return new Builder(ctx);
    }

    public Address getClient() {
        return client;
    }

    public Address getServer() {
        return server;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public HttpVersion getVersion() {
        return version;
    }

    public String getHost() {
        return host;
    }

    public String getPath() {
        return path;
    }

    public long getRequestBodySize() {
        return requestBodySize;
    }

    public long getRequestTime() {
        return requestTime;
    }

    public HttpResponseStatus getStatus() {
        return status;
    }

    public String getContentType() {
        return contentType;
    }

    public long getResponseBodySize() {
        return responseBodySize;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public long getTimeSpent() {
        return timeSpent;
    }

    public static class Builder {
        private Address client;
        private Address server;

        // request
        private HttpMethod method;
        private HttpVersion version;
        private String host;
        private String path;
        private long requestBodySize;
        private long requestTime;

        // response
        private HttpResponseStatus status;
        private String contentType;
        private long responseBodySize;
        private long responseTime;

        private Builder(ConnectionContext ctx) {
            client = ctx.getClientAddr();
            server = ctx.getServerAddr();
        }

        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder version(HttpVersion version) {
            this.version = version;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder requestBodySize(long requestBodySize) {
            this.requestBodySize = requestBodySize;
            return this;
        }

        public Builder addRequestBodySize(long delta) {
            this.requestBodySize += delta;
            return this;
        }

        public Builder requestTime(long requestTime) {
            this.requestTime = requestTime;
            return this;
        }

        public Builder status(HttpResponseStatus status) {
            this.status = status;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder responseBodySize(long responseBodySize) {
            this.responseBodySize = responseBodySize;
            return this;
        }

        public Builder addResponseBodySize(long delta) {
            this.responseBodySize += delta;
            return this;
        }

        public Builder responseTime(long responseTime) {
            this.responseTime = responseTime;
            return this;
        }

        public HttpEvent build() {
            return new HttpEvent(this);
        }
    }
}
