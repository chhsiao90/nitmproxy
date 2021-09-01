package com.github.chhsiao90.nitmproxy.testing;

import io.netty.buffer.ByteBuf;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.InstanceOfAssertFactory;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.Assert.*;

public class ByteBufAssert extends AbstractObjectAssert<ByteBufAssert, ByteBuf> {
    public ByteBufAssert(ByteBuf actual) {
        super(actual, ByteBufAssert.class);
    }

    public static InstanceOfAssertFactory<ByteBuf, ByteBufAssert> asByteBuf() {
        return new InstanceOfAssertFactory<>(ByteBuf.class, ByteBufAssert::new);
    }

    public ByteBufAssert hasContent(String expect) {
        assertEquals(expect, actual.toString(UTF_8));
        return this;
    }

    public void release() {
        actual.release();
    }
}
