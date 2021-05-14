package com.github.chhsiao90.nitmproxy.event;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import io.netty.buffer.ByteBuf;

public class ForwardEvent {
    private Address client;
    private Address server;

    private long requestBodySize;
    private long requestTime;

    // response
    private long responseBodySize;
    private long responseTime;

    private long timeSpent;

    private ForwardEvent(Builder builder) {
        client = builder.client;
        server = builder.server;

        requestBodySize = builder.requestBodySize;
        requestTime = builder.requestTime;

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

    public long getRequestBodySize() {
        return requestBodySize;
    }

    public long getRequestTime() {
        return requestTime;
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
        private long requestBodySize;
        private long requestTime;

        // response
        private long responseBodySize;
        private long responseTime;

        private Builder(ConnectionContext ctx) {
            client = ctx.getClientAddr();
            server = ctx.getServerAddr();
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

        public ForwardEvent build() {
            return new ForwardEvent(this);
        }
    }
}
