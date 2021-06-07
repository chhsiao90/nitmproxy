package com.github.chhsiao90.nitmproxy.handler.protocol.tls;

import com.github.chhsiao90.nitmproxy.event.AlpnCompletionEvent;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslClientHelloHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAlpnHandler<T> extends SslClientHelloHandler<T> {

    private static List<String> extractAlpnProtocols(ByteBuf in) {
        // See https://tools.ietf.org/html/rfc5246#section-7.4.1.2
        //
        // Decode the ssl client hello packet.
        //
        // struct {
        //    ProtocolVersion client_version;
        //    Random random;
        //    SessionID session_id;
        //    CipherSuite cipher_suites<2..2^16-2>;
        //    CompressionMethod compression_methods<1..2^8-1>;
        //    select (extensions_present) {
        //        case false:
        //            struct {};
        //        case true:
        //            Extension extensions<0..2^16-1>;
        //    };
        // } ClientHello;
        //

        // We have to skip bytes until SessionID (which sum to 34 bytes in this case).
        int offset = in.readerIndex();
        int endOffset = in.writerIndex();
        offset += 34;

        if (endOffset - offset >= 6) {
            final int sessionIdLength = in.getUnsignedByte(offset);
            offset += sessionIdLength + 1;

            final int cipherSuitesLength = in.getUnsignedShort(offset);
            offset += cipherSuitesLength + 2;

            final int compressionMethodLength = in.getUnsignedByte(offset);
            offset += compressionMethodLength + 1;

            final int extensionsLength = in.getUnsignedShort(offset);
            offset += 2;
            final int extensionsLimit = offset + extensionsLength;

            // Extensions should never exceed the record boundary.
            if (extensionsLimit <= endOffset) {
                while (extensionsLimit - offset >= 4) {
                    final int extensionType = in.getUnsignedShort(offset);
                    offset += 2;

                    final int extensionLength = in.getUnsignedShort(offset);
                    offset += 2;

                    if (extensionsLimit - offset < extensionLength) {
                        break;
                    }

                    // Alpn
                    // See https://tools.ietf.org/html/rfc7301
                    if (extensionType == 16) {
                        offset += 2;
                        if (extensionsLimit - offset < 3) {
                            break;
                        }

                        final int extensionLimit = offset + extensionLength;
                        List<String> protocols = new ArrayList<>();
                        while (offset + 8 < extensionLimit) {
                            final short protocolLength = in.getUnsignedByte(offset);
                            offset += 1;

                            if (extensionLimit - offset < protocolLength) {
                                break;
                            }

                            final String protocol = in.toString(offset, protocolLength, CharsetUtil.US_ASCII);
                            offset += protocolLength;
                            protocols.add(protocol);
                        }
                        return protocols;
                    }

                    offset += extensionLength;
                }
            }
        }
        return null;
    }

    private List<String> protocols;

    @Override
    protected Future<T> lookup(ChannelHandlerContext ctx, ByteBuf clientHello) throws Exception {
        protocols = clientHello == null? null : extractAlpnProtocols(clientHello);

        return lookup(ctx, protocols);
    }

    protected abstract Future<T> lookup(ChannelHandlerContext ctx, List<String> protocols)
            throws Exception;

    @Override
    protected void onLookupComplete(ChannelHandlerContext ctx, Future<T> future) throws Exception {
        try {
            onLookupComplete(ctx, protocols, future);
        } finally {
            fireAlpnCompletionEvent(ctx, protocols, future);
        }
    }

    protected abstract void onLookupComplete(ChannelHandlerContext ctx,
                                             List<String> protocols, Future<T> future) throws Exception;

    private static void fireAlpnCompletionEvent(ChannelHandlerContext ctx, List<String> protocols,
                                                Future<?> future) {
        Throwable cause = future.cause();
        if (cause == null) {
            ctx.fireUserEventTriggered(new AlpnCompletionEvent(protocols));
        } else {
            ctx.fireUserEventTriggered(new AlpnCompletionEvent(protocols, cause));
        }
    }
}
