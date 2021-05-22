package com.github.chhsiao90.nitmproxy;

public interface NitmProxyStatusListener {

    default void onStart() {
    }

    default void onStop() {
    }
}
