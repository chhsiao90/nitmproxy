package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.listener.NitmProxyListener.Empty;

import java.util.function.Predicate;

public interface NitmProxyListenerProvider {
    NitmProxyListenerProvider EMPTY = new Singleton(new Empty());

    NitmProxyListener create();

    Class<? extends NitmProxyListener> listenerClass();

    static NitmProxyListenerProvider empty() {
        return EMPTY;
    }

    static NitmProxyListenerProvider singleton(NitmProxyListener listener) {
        return new Singleton(listener);
    }

    static Predicate<NitmProxyListenerProvider> match(Class<?> listenerClass) {
        if (NitmProxyListener.class.isAssignableFrom(listenerClass)) {
            return new MatchListenerPredicate(listenerClass);
        }
        return new MatchProviderPredicate(listenerClass);
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

        @Override
        public Class<? extends NitmProxyListener> listenerClass() {
            return listener.getClass();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Singleton singleton = (Singleton) o;

            return listener.equals(singleton.listener);
        }

        @Override
        public int hashCode() {
            return listener.hashCode();
        }

        @Override
        public String toString() {
            return "singleton(" + listener + ")";
        }
    }

    class MatchProviderPredicate implements Predicate<NitmProxyListenerProvider> {
        private Class<?> providerClass;

        public MatchProviderPredicate(Class<?> providerClass) {
            this.providerClass = providerClass;
        }

        @Override
        public boolean test(NitmProxyListenerProvider listener) {
            return providerClass.equals(listener.getClass());
        }

        @Override
        public String toString() {
            return "Predicate[providerClass=" + providerClass + "]";
        }
    }

    class MatchListenerPredicate implements Predicate<NitmProxyListenerProvider> {
        private Class<?> listenerClass;

        public MatchListenerPredicate(Class<?> listenerClass) {
            this.listenerClass = listenerClass;
        }

        @Override
        public boolean test(NitmProxyListenerProvider listener) {
            return listenerClass.equals(listener.listenerClass());
        }

        @Override
        public String toString() {
            return "Predicate[listenerClass=" + listenerClass + "]";
        }
    }
}
