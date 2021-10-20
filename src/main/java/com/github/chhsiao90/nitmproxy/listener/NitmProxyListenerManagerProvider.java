package com.github.chhsiao90.nitmproxy.listener;

import java.util.List;

import static com.google.common.collect.ImmutableList.*;

public class NitmProxyListenerManagerProvider implements NitmProxyListenerProvider {

    private final List<NitmProxyListenerProvider> providers;

    public NitmProxyListenerManagerProvider(
            List<NitmProxyListenerProvider> providers) {
        this.providers = providers;
    }

    @Override
    public NitmProxyListener create() {
        return new NitmProxyListenerManager(providers.stream()
                .map(NitmProxyListenerProvider::create)
                .collect(toImmutableList()));
    }
}
