[![Java CI](https://github.com/chhsiao90/nitmproxy/actions/workflows/ci.yml/badge.svg)](https://github.com/chhsiao90/nitmproxy/actions/workflows/ci.yml)

# Netty in the Middle

An experimental proxy server based on [netty](https://github.com/netty/netty).
That want to show how fast the netty is, and how the API design of netty is pretty.

## Start nitmproxy
```
> ./nitmproxy.sh --help
usage: nitmproxy [--cert <CERTIFICATE>] [--clientNoHttp2] [-h <HOST>] [-k]
       [--key <KEY>] [-m <MODE>] [-p <PORT>] [--serverNoHttp2]
    --cert <CERTIFICATE>   x509 certificate used by server(*.pem),
                           default: server.pem
 -h,--host <HOST>          listening host, default: 127.0.0.1
 -k,--insecure             not verify on server certificate
    --key <KEY>            key used by server(*.pem), default: key.pem
 -m,--mode <MODE>          proxy mode(HTTP, SOCKS, TRANSPARENT), default: HTTP
 -p,--port <PORT>          listening port, default: 8080
```

## Features

### Support Proxy
- HTTP Proxy
- HTTP Proxy (Tunnel)
- Socks Proxy
- Transparent Proxy

### Support Protocol
- HTTP/1
- HTTP/2
- WebSocket (WIP)
- TLS

### Support Functionality
- Display network traffic
- Modify network traffic (WIP)

## Development

### Coding Style

We are using same coding style with netty, please follow the instructions from the [netty#Setting up development environment](https://netty.io/wiki/setting-up-development-environment.html) to setup.

## FAQ

### Android

The built-in [Conscrypt](https://github.com/google/conscrypt) in the Android is not compatible with [Netty](https://github.com/netty/netty). The easiest way to fix is to add Conscrypt manually.

**Add conscrypt-android dependency**

https://search.maven.org/artifact/org.conscrypt/conscrypt-android

**Configure Conscrypt SSL provider**

```java
config.setSslProvider(Conscrypt.newProvider());
```
