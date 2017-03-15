package com.github.chhsiaoninety.nitm.nitmproxy;

public enum ProxyMode {
    SOCKS,
    HTTP;

    public static ProxyMode of(String name) {
        try {
            return ProxyMode.valueOf(name);
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal proxy mode: " + name);
        }
    }
}
