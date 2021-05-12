package com.github.chhsiao90.nitmproxy.handler.protocol;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.Protocols;
import com.github.chhsiao90.nitmproxy.exception.NitmProxyException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

public class ProtocolSelectHandler extends ByteToMessageDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolSelectHandler.class);

    private final ConnectionContext connectionContext;

    public ProtocolSelectHandler(ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        String protocol = connectionContext
                .config()
                .getDetectors()
                .stream()
                .flatMap(detector -> detector.detect(in).map(Stream::of).orElse(Stream.empty()))
                .findFirst()
                .orElse(Protocols.FORWARD);
        connectionContext.tlsCtx().protocolPromise().setSuccess(protocol);

        try {
            ctx.pipeline().addAfter(ctx.name(), null, connectionContext.provider().frontendHandler(protocol));
            ctx.pipeline().remove(this);
        } catch (NitmProxyException e) {
            LOGGER.error("{} : Unsupported protocol", connectionContext);
            ctx.close();
        }
    }
}
