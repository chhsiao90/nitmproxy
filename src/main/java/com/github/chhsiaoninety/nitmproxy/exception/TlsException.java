package com.github.chhsiaoninety.nitmproxy.exception;

public class TlsException extends NitmProxyException {

  public TlsException(String message) {
    super(message);
  }

  public TlsException(String message, Throwable cause) {
    super(message, cause);
  }
}
