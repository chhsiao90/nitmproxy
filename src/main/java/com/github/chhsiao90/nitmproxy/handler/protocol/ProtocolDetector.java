package com.github.chhsiao90.nitmproxy.handler.protocol;

import io.netty.buffer.ByteBuf;

import java.util.Optional;

public interface ProtocolDetector {

    Optional<String> detect(ByteBuf msg);

}
