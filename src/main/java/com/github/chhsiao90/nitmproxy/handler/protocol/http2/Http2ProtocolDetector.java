package com.github.chhsiao90.nitmproxy.handler.protocol.http2;

import com.github.chhsiao90.nitmproxy.Protocols;
import com.github.chhsiao90.nitmproxy.handler.protocol.AbstractHttpProtocolDetector;
import io.netty.buffer.ByteBuf;

import java.util.Optional;
import java.util.regex.Pattern;

public class Http2ProtocolDetector extends AbstractHttpProtocolDetector {

    private static final int MAX_READ_BYTES = 100;
    private static final Pattern HTTP_FIRST_LINE = Pattern.compile("[A-Z]+\\s\\S+\\sHTTP/2\\.0");

    public static final Http2ProtocolDetector INSTANCE = new Http2ProtocolDetector();

    public Http2ProtocolDetector() {
        super(MAX_READ_BYTES, HTTP_FIRST_LINE);
    }

    @Override
    public Optional<String> detect(ByteBuf msg) {
        return super.detect(msg);
    }

    @Override
    public String toString() {
        return Protocols.HTTP_2;
    }
}
