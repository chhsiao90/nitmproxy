package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.event.HttpEvent;

public interface HttpEventListener {
    void onHttpEvent(HttpEvent event);
}