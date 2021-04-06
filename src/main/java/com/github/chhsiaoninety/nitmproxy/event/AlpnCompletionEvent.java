package com.github.chhsiaoninety.nitmproxy.event;

import java.util.List;

public class AlpnCompletionEvent {

  private List<String> protocols;
  private Throwable cause;

  public AlpnCompletionEvent(List<String> protocols) {
    this(protocols, null);
  }

  public AlpnCompletionEvent(List<String> protocols, Throwable cause) {
    this.protocols = protocols;
    this.cause = cause;
  }

  public List<String> protocols() {
    return protocols;
  }

  public boolean isSuccess() {
    return cause == null;
  }

  public Throwable cause() {
    return cause;
  }

}
