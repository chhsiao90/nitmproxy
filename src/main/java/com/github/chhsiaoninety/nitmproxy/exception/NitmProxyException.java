package com.github.chhsiaoninety.nitmproxy.exception;

public class NitmProxyException extends RuntimeException {

  public NitmProxyException(String message) {
    super(message);
  }

  public NitmProxyException(String message, Throwable cause) {
    super(message, cause);
  }
}
