# Netty in the Middle

An experimental proxy server based on [netty](https://github.com/netty/netty).
That want to show how fast the netty is, and how the API design of netty is pretty.

### Start nitmproxy
```
> ./nitmproxy.sh --help
usage: nitmproxy [--cert <CERTIFICATE>] [--clientNoHttp2] [-h <HOST>] [-k]
       [--key <KEY>] [-m <MODE>] [-p <PORT>] [--serverNoHttp2]
    --cert <CERTIFICATE>   x509 certificate used by server(*.pem),
                           default: server.pem
    --clientNoHttp2        disable http2 for client
 -h,--host <HOST>          listening host, default: 127.0.0.1
 -k,--insecure             not verify on server certificate
    --key <KEY>            key used by server(*.pem), default: key.pem
 -m,--mode <MODE>          proxy mode(HTTP, SOCKS), default: HTTP
 -p,--port <PORT>          listening port, default: 8080
    --serverNoHttp2        disable http2 for server
```

### Support Proxy
- HTTP Proxy
- HTTP Proxy (Tunnel)
- Socks Proxy

### Support Protocol
- HTTP/1
- HTTP/2
- WebSocket (WIP)
- TLS

### Support Functionality
- Display network traffic
- Modify network traffic (WIP)
