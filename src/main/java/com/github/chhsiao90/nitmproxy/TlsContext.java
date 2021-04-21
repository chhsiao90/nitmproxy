package com.github.chhsiao90.nitmproxy;

import com.github.chhsiao90.nitmproxy.exception.TlsException;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.util.concurrent.Promise;

import java.util.List;

import static java.util.Collections.*;

public class TlsContext {

    private boolean enabled = true;
    private Promise<List<String>> protocols;
    private Promise<String> protocol;

    public TlsContext protocols(Promise<List<String>> protocols) {
        this.protocols = protocols;
        return this;
    }

    /**
     * Get the ALPN protocols sent from the client.
     *
     * @return the protocols
     */
    public List<String> protocols() {
        if (!protocols.isDone()) {
            throw new TlsException("Alpn protocols not resolved before accessing");
        }
        return protocols.getNow();
    }

    public Promise<List<String>> protocolsPromise() {
        return protocols;
    }

    public TlsContext protocol(Promise<String> protocol) {
        this.protocol = protocol;
        return this;
    }

    /**
     * Get the negotiated protocol.
     *
     * @return the protocol
     */
    public String protocol() {
        if (!protocol.isDone()) {
            throw new TlsException("Alpn protocol not negotiated before accessing");
        }
        return protocol.getNow();
    }

    public Promise<String> protocolPromise() {
        return protocol;
    }

    public boolean isNegotiated() {
        return protocol.isDone();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void disableTls() {
        enabled = false;
        protocols.setSuccess(singletonList(ApplicationProtocolNames.HTTP_1_1));
        protocol.setSuccess(ApplicationProtocolNames.HTTP_1_1);
    }
}
