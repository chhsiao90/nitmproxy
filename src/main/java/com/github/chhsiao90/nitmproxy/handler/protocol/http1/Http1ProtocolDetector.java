package com.github.chhsiao90.nitmproxy.handler.protocol.http1;

import com.github.chhsiao90.nitmproxy.Protocols;
import com.github.chhsiao90.nitmproxy.handler.protocol.AbstractHttpProtocolDetector;
import io.netty.buffer.ByteBuf;

import java.util.Optional;
import java.util.regex.Pattern;

public class Http1ProtocolDetector extends AbstractHttpProtocolDetector {

    private static final int MAX_READ_BYTES = 100;
    private static final Pattern HTTP_FIRST_LINE = Pattern.compile("[A-Z]+\\s\\S+\\sHTTP/1\\.1");

    public static final Http1ProtocolDetector INSTANCE = new Http1ProtocolDetector();

    public Http1ProtocolDetector() {
        super(MAX_READ_BYTES, HTTP_FIRST_LINE);
    }

    @Override
    public Optional<String> detect(ByteBuf msg) {
        return super.detect(msg);
    }

    @Override
    public String toString() {
        return Protocols.HTTP_1;
    }
}

