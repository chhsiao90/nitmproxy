package com.github.chhsiao90.nitmproxy;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Address {

    private static final Pattern PATTERN = Pattern.compile("^([a-zA-Z0-9.\\-_]+):(\\d+)");

    private String host;
    private int port;

    public Address(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Resolve the address.
     *
     * @param address the address
     * @return the resolved address
     */
    public static Address resolve(String address) {
        Matcher matcher = PATTERN.matcher(address);
        if (matcher.find()) {
            return new Address(matcher.group(1), Integer.parseInt(matcher.group(2)));
        } else {
            throw new IllegalArgumentException("Illegal address: " + address);
        }
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
