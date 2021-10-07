package com.github.chhsiao90.nitmproxy.util;

import com.github.chhsiao90.nitmproxy.handler.protocol.http2.Http2FrameWrapper;
import io.netty.buffer.ByteBufHolder;

public  class LogWrappers {

    private LogWrappers() {
    }

    public static Object description(Object msg) {
        if (msg instanceof Http2FrameWrapper) {
            return msg;
        }
        if (msg instanceof ByteBufHolder) {
            return new ByteBufHolderDescription((ByteBufHolder) msg);
        }
        return msg;
    }

    public static Format format(String format, Object... args) {
        return new Format(format, args);
    }

    public static class ClassName {
        private final Object msg;

        public ClassName(Object msg) {
            this.msg = msg;
        }

        @Override
        public String toString() {
            return msg.getClass().getSimpleName();
        }
    }

    public static class ByteBufHolderDescription {
        private final ByteBufHolder msg;

        public ByteBufHolderDescription(ByteBufHolder msg) {
            this.msg = msg;
        }

        @Override
        public String toString() {
            return msg.getClass().getSimpleName()
                    + "(length="
                    + msg.content().readableBytes()
                    + ")";
        }
    }

    public static class Format {
        private final String format;
        private final Object[] args;

        public Format(String format, Object[] args) {
            this.format = format;
            this.args = args;
        }

        @Override
        public String toString() {
            return String.format(format, args);
        }
    }
}
