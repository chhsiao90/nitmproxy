package com.github.chhsiao90.nitmproxy.util;

public  class LogWrappers {

    private LogWrappers() {
    }

    public static ClassName className(Object msg) {
        return new ClassName(msg);
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
