package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.event.HttpEvent;

import java.util.ArrayList;
import java.util.List;

public class NitmProxyListenerManager implements HttpEventListener {

    private List<HttpEventListener> httpEventListeners = new ArrayList<>();

    public NitmProxyListenerManager() {
        httpEventListeners.add(new HttpEventLogger());
    }

    @Override
    public void onHttpEvent(HttpEvent event) {
        httpEventListeners.forEach(listener -> listener.onHttpEvent(event));
    }
}
