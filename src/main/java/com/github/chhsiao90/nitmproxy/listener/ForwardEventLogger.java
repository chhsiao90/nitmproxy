package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.event.ForwardEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForwardEventLogger implements ForwardListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForwardEventLogger.class);

    @Override
    public void onForwardEvent(ConnectionContext connectionContext, ForwardEvent event) {
        LOGGER.info("{} {} {} {} {}",
                event.getTimeSpent(),
                event.getServer(),
                event.getClient(),
                event.getRequestBodySize(),
                event.getResponseBodySize());
    }

    @Override
    public void onForwardRequest(ConnectionContext connectionContext, ByteBuf byteBuf) {
        LOGGER.debug("{}", ByteBufUtil.hexDump(byteBuf));
    }

    @Override
    public void onForwardResponse(ConnectionContext connectionContext, ByteBuf byteBuf) {
        LOGGER.debug("{}", ByteBufUtil.hexDump(byteBuf));
    }
}
