package com.github.chhsiao90.nitmproxy;

import com.github.chhsiao90.nitmproxy.exception.TlsException;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import io.netty.util.concurrent.Promise;

public class TlsContext {

  private boolean enabled = true;
  private final static boolean SUPPORT_ALPN;
  private Promise<List<String>> protocols;
  private Promise<String> protocol;
  private final static List<String> TLS_PROTOCOLS;

  static {
    SUPPORT_ALPN = Arrays.stream(SSLEngine.class.getDeclaredMethods()).anyMatch(m -> m.getName().equals("getApplicationProtocol"));
    try {
      TLS_PROTOCOLS = Arrays.asList(SSLContext.getDefault().createSSLEngine().getSupportedProtocols());
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Could not init default SSLContext.", e);
    }
  }

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

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isSupportAlpn() {
    return SUPPORT_ALPN;
  }

  public List<String> getTlsProtocols() {
    return TLS_PROTOCOLS;
  }
}
