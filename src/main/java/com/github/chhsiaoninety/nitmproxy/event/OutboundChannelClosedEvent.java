package com.github.chhsiaoninety.nitmproxy.event;

import com.github.chhsiaoninety.nitmproxy.ConnectionInfo;

public class OutboundChannelClosedEvent {
    private ConnectionInfo connectionInfo;
    private boolean client;

    public OutboundChannelClosedEvent(ConnectionInfo connectionInfo, boolean client) {
        this.connectionInfo = connectionInfo;
    }

    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    public boolean isClient() {
        return client;
    }

    @Override
    public String toString() {
        return String.format("%s : channelClosed", connectionInfo.toString(client));
    }
}
