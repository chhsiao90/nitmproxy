package com.github.chhsiao90.nitmproxy.ws;

public class WebSocketContext {
    private String path;

    public WebSocketContext path(String path) {
        this.path = path;
        return this;
    }

    public String path() {
        return path;
    }
}
