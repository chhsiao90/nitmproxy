package com.github.chhsiao.nitm.nitmproxy;

public class ConnectionInfo {
    private Address clientAddr;
    private Address serverAddr;

    public ConnectionInfo(Address clientAddr) {
        this.clientAddr = clientAddr;
    }

    public ConnectionInfo(Address clientAddr, Address serverAddr) {
        this.clientAddr = clientAddr;
        this.serverAddr = serverAddr;
    }

    public Address getClientAddr() {
        return clientAddr;
    }

    public void setClientAddr(Address clientAddr) {
        this.clientAddr = clientAddr;
    }

    public Address getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(Address serverAddr) {
        this.serverAddr = serverAddr;
    }

    @Override
    public String toString() {
        return String.format("[Client (%s)] <=> [Server (%s)]",
                             clientAddr, serverAddr);
    }
}
