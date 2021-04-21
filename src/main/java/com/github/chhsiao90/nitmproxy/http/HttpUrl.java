package com.github.chhsiao90.nitmproxy.http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.*;

public class HttpUrl {

    private static final Pattern PATTERN = Pattern
            .compile("(https?)://([a-zA-Z0-9.\\-]+)(:(\\d+))?(/.*)");

    private String scheme;
    private String host;
    private int port;
    private String path;

    /**
     * Create http url.
     *
     * @param scheme the scheme
     * @param host   the host
     * @param port   the port
     * @param path   the path
     */
    public HttpUrl(String scheme, String host, int port, String path) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.path = path;
    }

    /**
     * Resolve the uri.
     *
     * @param uri the uri
     * @return the resolved http url
     */
    public static HttpUrl resolve(String uri) {
        Matcher matcher = PATTERN.matcher(uri);
        if (matcher.find()) {
            String scheme = matcher.group(1);
            String host = matcher.group(2);
            int port = resolvePort(scheme, matcher.group(4));
            String path = matcher.group(5);
            return new HttpUrl(scheme, host, port, path);
        } else {
            throw new IllegalArgumentException("Illegal path: " + uri);
        }
    }

    private static int resolvePort(String scheme, String port) {
        if (isNullOrEmpty(port)) {
            return "https".equals(scheme)? 443 : 80;
        }
        return Integer.parseInt(port);
    }

    public static String scheme(boolean tlsEnabled) {
        return tlsEnabled? "https" : "http";
    }

    public String getScheme() {
        return scheme;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return String.format("%s://%s:%d%s", scheme, host, port, path);
    }
}
