package com.github.chhsiaoninety.nitmproxy;

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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConnectionInfo that = (ConnectionInfo) o;

        if (clientAddr != null? !clientAddr.equals(that.clientAddr) : that.clientAddr != null) {
            return false;
        }
        return serverAddr != null? serverAddr.equals(that.serverAddr) : that.serverAddr == null;

    }

    @Override
    public int hashCode() {
        int result = clientAddr != null? clientAddr.hashCode() : 0;
        result = 31 * result + (serverAddr != null? serverAddr.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("[Client (%s)] <=> [Server (%s)]",
                             clientAddr, serverAddr);
    }

    public String toString(boolean client) {
        if (client) {
            return String.format("[Client (%s)] <=> [PROXY]", clientAddr);
        } else {
            return String.format("[PROXY] <=> [Server (%s)]", serverAddr);
        }
    }
}
