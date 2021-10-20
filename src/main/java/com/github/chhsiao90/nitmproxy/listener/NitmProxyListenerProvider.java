package com.github.chhsiao90.nitmproxy.listener;

public interface NitmProxyListenerProvider {
    NitmProxyListener create();

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
