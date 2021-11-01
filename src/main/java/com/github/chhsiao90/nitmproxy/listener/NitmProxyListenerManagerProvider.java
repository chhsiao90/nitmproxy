package com.github.chhsiao90.nitmproxy.listener;

import static com.google.common.collect.ImmutableList.*;

public class NitmProxyListenerManagerProvider implements NitmProxyListenerProvider {

    private final NitmProxyListenerStore listenerStore;

    public NitmProxyListenerManagerProvider(NitmProxyListenerStore listenerStore) {
        this.listenerStore = listenerStore;
    }

    @Override
    public NitmProxyListener create() {
        return new NitmProxyListenerManager(listenerStore.getListeners().stream()
                .map(NitmProxyListenerProvider::create)
                .collect(toImmutableList()));
    }

    @Override
    public Class<? extends NitmProxyListener> listenerClass() {
        return NitmProxyListenerManager.class;
    }
}
