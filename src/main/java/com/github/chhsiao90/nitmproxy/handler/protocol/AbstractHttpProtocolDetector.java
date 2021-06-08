package com.github.chhsiao90.nitmproxy.handler.protocol;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

public abstract class AbstractHttpProtocolDetector implements ProtocolDetector {

    private final int maxBytes;
    private final Pattern pattern;

    public AbstractHttpProtocolDetector(int maxBytes, Pattern pattern) {
        this.pattern = pattern;
        this.maxBytes = maxBytes;
    }

    @Override
    public Optional<String> detect(ByteBuf msg) {
        int readBytes = Math.min(maxBytes, msg.readableBytes());
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
        if (pattern.matcher(line).matches()) {
            return Optional.of(toString());
        }
        return Optional.empty();
    }

    @Override
    public abstract String toString();
}
