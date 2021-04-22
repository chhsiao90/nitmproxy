package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.event.HttpEvent;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpEventLogger implements HttpListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpEventLogger.class);

    @Override
    public void onHttpEvent(HttpEvent event) {
        if (!HttpMethod.CONNECT.equals(event.getMethod())) {
            LOGGER.info("{} {} {} {} {} {} {} {}",
                    event.getResponseTime() - event.getRequestTime(),
                    event.getHost(),
                    event.getStatus(),
                    event.getResponseBodySize(),
                    event.getMethod(),
                    event.getPath(),
                    event.getVersion(),
                    event.getHost());
        }
    }
}
