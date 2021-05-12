package com.github.chhsiao90.nitmproxy.handler.protocol.http1;

import com.github.chhsiao90.nitmproxy.Protocols;
import com.github.chhsiao90.nitmproxy.handler.protocol.ProtocolDetector;
import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

public class Http1ProtocolDetector implements ProtocolDetector {

    private static final int MAX_READ_BYTES = 100;
    private static final Pattern HTTP_FIRST_LINE = Pattern.compile("[A-Z]+\\s\\S+\\sHTTP/1\\.1");

    public static final Http1ProtocolDetector INSTANCE = new Http1ProtocolDetector();

    @Override
    public Optional<String> detect(ByteBuf msg) {
        int readBytes = Math.min(MAX_READ_BYTES, msg.readableBytes());
        if (readBytes == 0) {
            return Optional.empty();
        }
        byte[] bytes = new byte[readBytes];
        for (int pos = 0; pos < readBytes; pos++) {
            bytes[pos] = msg.getByte(pos);
            if (bytes[pos] == '\r') {
                bytes = Arrays.copyOf(bytes, pos);
                break;
            }
        }
        String line = new String(bytes);
        if (HTTP_FIRST_LINE.matcher(line).matches()) {
            return Optional.of(Protocols.HTTP_1);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return Protocols.HTTP_1;
    }
}
