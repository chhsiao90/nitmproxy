package com.github.chhsiao90.nitmproxy;

import java.util.Objects;

public class Address {
    private String host;
    private int port;

    public Address(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Address address = (Address) o;

        if (port != address.port) {
            return false;
        }
        return Objects.equals(host, address.host);
    }

    @Override
    public int hashCode() {
        int result = host != null? host.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s:%d", host, port);
    }
}
