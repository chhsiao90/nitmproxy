package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.testing.NitmProxyListenerStoreAssert;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.api.Condition;
import org.junit.Test;

import java.util.function.Consumer;

import static com.google.common.base.Predicates.*;
import static org.assertj.core.api.Assertions.*;


public class NitmProxyListenerStoreTest {

    @Test
    public void shouldAddBefore() {
        assertThat(configure(store -> store.addBefore(Pivot.class, new ListenerA())))
                .hasListeners(listener(ListenerA.class), listener(Pivot.class));
        assertThat(configure(store -> store.addBefore(PivotProvider.class, new ProviderA())))
                .hasListeners(listener(ProviderA.class), listener(Pivot.class));
        assertThat(configure(store -> store.addBefore(alwaysTrue(), new ListenerA())))
                .hasListeners(listener(ListenerA.class), listener(Pivot.class));
    }

    @Test
    public void shouldAddAfter() {
        assertThat(configure(store -> store.addAfter(Pivot.class, new ListenerA())))
                .hasListeners(listener(Pivot.class), listener(ListenerA.class));
        assertThat(configure(store -> store.addAfter(PivotProvider.class, new ProviderA())))
                .hasListeners(listener(Pivot.class), listener(ProviderA.class));
        assertThat(configure(store -> store.addAfter(alwaysTrue(), new ListenerA())))
                .hasListeners(listener(Pivot.class), listener(ListenerA.class));
    }

    @Test
    public void shouldAddFirst() {
        assertThat(configure(store -> store.addFirst(new ListenerA())))
                .hasListeners(listener(ListenerA.class), listener(Pivot.class));
        assertThat(configure(store -> store.addFirst(new ProviderA())))
                .hasListeners(listener(ProviderA.class), listener(Pivot.class));
    }

    @Test
    public void shouldAddLast() {
        assertThat(configure(store -> store.addLast(new ListenerA())))
                .hasListeners(listener(Pivot.class), listener(ListenerA.class));
        assertThat(configure(store -> store.addLast(new ProviderA())))
                .hasListeners(listener(Pivot.class), listener(ProviderA.class));
    }

    private static AssertProvider<NitmProxyListenerStoreAssert> configure(Consumer<NitmProxyListenerStore> consumer) {
        NitmProxyListenerStore listenerStore = new NitmProxyListenerStore();
        listenerStore.addLast(new PivotProvider());
        consumer.accept(listenerStore);
        return () -> new NitmProxyListenerStoreAssert(listenerStore);
    }

    private static Condition<NitmProxyListenerProvider> listener(Class<?> type) {
        return new Condition<>(NitmProxyListenerProvider.match(type), type.getTypeName());
    }

    private static class Pivot implements NitmProxyListener {}

    private static class PivotProvider implements NitmProxyListenerProvider {
        private static final Pivot INSTANCE = new Pivot();

        @Override
        public NitmProxyListener create() {
            return INSTANCE;
        }

        @Override
        public Class<? extends NitmProxyListener> listenerClass() {
            return Pivot.class;
        }
    }

    private static class ListenerA implements NitmProxyListener {}

    private static class ProviderA implements NitmProxyListenerProvider {
        @Override
        public NitmProxyListener create() {
            return new ListenerA();
        }

        @Override
        public Class<? extends NitmProxyListener> listenerClass() {
            return ListenerA.class;
        }
    }
}
