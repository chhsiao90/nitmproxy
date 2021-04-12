package com.github.chhsiao90.nitmproxy.exception;

public class NitmProxyException extends RuntimeException {

  public NitmProxyException(String message) {
    super(message);
  }

  public NitmProxyException(String message, Throwable cause) {
    super(message, cause);
  }
}
