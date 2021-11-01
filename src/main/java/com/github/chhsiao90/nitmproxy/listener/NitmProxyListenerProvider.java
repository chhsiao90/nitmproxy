package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.listener.NitmProxyListener.Empty;

public interface NitmProxyListenerProvider {
    NitmProxyListenerProvider EMPTY = new Singleton(new Empty());

    NitmProxyListener create();

    static NitmProxyListenerProvider empty() {
        return EMPTY;
    }

    static NitmProxyListenerProvider singleton(NitmProxyListener listener) {
        return new Singleton(listener);
    }

    class Singleton implements NitmProxyListenerProvider {
        private NitmProxyListener listener;

        public Singleton(NitmProxyListener listener) {
            this.listener = listener;
        }

        @Override
        public NitmProxyListener create() {
            return listener;
        }
    }
}
