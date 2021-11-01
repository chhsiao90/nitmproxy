package com.github.chhsiao90.nitmproxy.exception;

import java.util.function.Supplier;

import static java.lang.String.*;

public class NitmProxyException extends RuntimeException {

    public NitmProxyException(String message) {
        super(message);
    }

    public NitmProxyException(String message, Throwable cause) {
        super(message, cause);
    }

    public static Supplier<NitmProxyException> toThrow(String message, Object... args) {
        return () -> new NitmProxyException(format(message, args));
    }
}
