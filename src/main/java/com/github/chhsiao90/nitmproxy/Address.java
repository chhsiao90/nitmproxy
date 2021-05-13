package com.github.chhsiao90.nitmproxy;

import com.github.chhsiao90.nitmproxy.exception.NitmProxyException;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Address {

    private static final Pattern PATTERN = Pattern.compile("^([a-zA-Z0-9.\\-_]+)(:\\d+)?");

    private String host;
    private int port;

    public Address(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static Address address(String host, int port) {
        return new Address(host, port);
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
            int port = Optional.ofNullable(matcher.group(2))
                               .map(group -> Integer.parseInt(group.substring(1)))
                               .orElseThrow(() -> new NitmProxyException("Invalid address: " + address));
            return new Address(matcher.group(1), port);
        } else {
            throw new NitmProxyException("Invalid address: " + address);
        }
    }

    /**
     * Resolve the address.
     *
     * @param address the address
     * @param defaultPort the default port
     * @return the resolved address
     */
    public static Address resolve(String address, int defaultPort) {
        Matcher matcher = PATTERN.matcher(address);
        if (matcher.find()) {
            int port = Optional.ofNullable(matcher.group(2))
                               .map(group -> Integer.parseInt(group.substring(1)))
                               .orElse(defaultPort);
            return new Address(matcher.group(1), port);
        } else {
            throw new NitmProxyException("Invalid address: " + address);
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
