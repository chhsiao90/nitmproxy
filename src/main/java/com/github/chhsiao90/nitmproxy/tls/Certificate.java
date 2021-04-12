package com.github.chhsiao90.nitmproxy.tls;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

public class Certificate {
    private KeyPair keyPair;
    private X509Certificate[] chain;

    public Certificate(KeyPair keyPair, X509Certificate... chain) {
        this.keyPair = keyPair;
        this.chain = chain;
    }

    public X509Certificate[] getChain() {
        return chain;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }
}
