package com.github.chhsiao90.nitmproxy.handler.protocol.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.handler.codec.http2.Http2DataFrame;

public class Http2DataFrameWrapper
        extends Http2FrameWrapper<Http2DataFrame>
        implements ByteBufHolder {

    public Http2DataFrameWrapper(int streamId, Http2DataFrame frame) {
        super(streamId, frame);
    }

    @Override
    public ByteBuf content() {
        return frame.content();
    }

    @Override
    public Http2DataFrameWrapper copy() {
        return replace(frame.content().copy());
    }

    @Override
    public Http2DataFrameWrapper duplicate() {
        return replace(frame.content().duplicate());
    }

    @Override
    public Http2DataFrameWrapper retainedDuplicate() {
        return replace(frame.content().retainedDuplicate());
    }

    @Override
    public Http2DataFrameWrapper replace(ByteBuf content) {
        return new Http2DataFrameWrapper(streamId, frame.replace(content));
    }

    @Override
    public int refCnt() {
        return frame.refCnt();
    }

    @Override
    public Http2DataFrameWrapper retain() {
        frame.retain();
        return this;
    }

    @Override
    public Http2DataFrameWrapper retain(int increment) {
        frame.retain(increment);
        return this;
    }

    @Override
    public Http2DataFrameWrapper touch() {
        frame.touch();
        return this;
    }

    @Override
    public Http2DataFrameWrapper touch(Object hint) {
        frame.touch(hint);
        return this;
    }

    @Override
    public boolean release() {
        return frame.release();
    }

    @Override
    public boolean release(int decrement) {
        return frame.release(decrement);
    }

    @Override
    public String toString() {
        return frame.name() + " Frame:" +
               " streamId=" + streamId +
               " endStream=" + frame.isEndStream() +
               " length=" + frame.content().readableBytes();
    }
}
