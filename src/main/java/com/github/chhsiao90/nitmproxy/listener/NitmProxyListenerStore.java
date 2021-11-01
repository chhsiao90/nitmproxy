package com.github.chhsiao90.nitmproxy.listener;

import com.github.chhsiao90.nitmproxy.exception.NitmProxyException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static com.github.chhsiao90.nitmproxy.listener.NitmProxyListenerProvider.*;

/**
 * This class provides more user-friendly api to configure the listeners.
 */
public class NitmProxyListenerStore {

    private List<NitmProxyListenerProvider> listeners;

    public NitmProxyListenerStore() {
        this(new ArrayList<>());
    }

    public NitmProxyListenerStore(
            List<NitmProxyListenerProvider> listeners) {
        this.listeners = listeners;
    }

    /**
     * Add the listener to the first place of the store.
     *
     * @param listener the listener
     * @return the store itself
     */
    public NitmProxyListenerStore addFirst(NitmProxyListener listener) {
        return addFirst(singleton(listener));
    }

    /**
     * Add the provider to the first place of the store.
     *
     * @param provider the listener
     * @return the store itself
     */
    public NitmProxyListenerStore addFirst(NitmProxyListenerProvider provider) {
        listeners.add(0, provider);
        return this;
    }

    /**
     * Add the listener to the place after the {@code target}.
     *
     * @param target the target class, can be a subclass of {@link NitmProxyListener}
     *               or {@link NitmProxyListenerProvider}
     * @param listener the listener
     * @return the store itself
     */
    public NitmProxyListenerStore addAfter(Class<?> target, NitmProxyListener listener) {
        return addAfter(target, singleton(listener));
    }

    /**
     * Add the provider to the place after the {@code target}.
     *
     * @param target the target class, can be a subclass of {@link NitmProxyListener}
     *               or {@link NitmProxyListenerProvider}
     * @param provider the listener provider
     * @return the store itself
     */
    public NitmProxyListenerStore addAfter(Class<?> target, NitmProxyListenerProvider provider) {
        return addAfter(NitmProxyListenerProvider.match(target), provider);
    }

    /**
     * Add the listener to the place after the place that {@code predicate} was matched.
     *
     * @param predicate the predicate to determine the place for the listener to be inserted
     * @param listener the listener
     * @return the store itself
     */
    public NitmProxyListenerStore addAfter(Predicate<NitmProxyListenerProvider> predicate, NitmProxyListener listener) {
        return addAfter(predicate, singleton(listener));
    }

    /**
     * Add the provider to the place after the place that {@code predicate} was matched.
     *
     * @param predicate the predicate to determine the place for the listener to be inserted
     * @param provider the provider
     * @return the store itself
     */
    public NitmProxyListenerStore addAfter(Predicate<NitmProxyListenerProvider> predicate,
            NitmProxyListenerProvider provider) {
        int matched = IntStream.range(0, listeners.size())
                               .filter(index -> predicate.test(listeners.get(index)))
                               .findFirst()
                               .orElseThrow(NitmProxyException.toThrow("Listener not exist in store: %s", predicate));
        listeners.add(matched + 1, provider);
        return this;
    }

    /**
     * Add the listener to the place before the {@code target}.
     *
     * @param target the target class, can be a subclass of {@link NitmProxyListener}
     *               or {@link NitmProxyListenerProvider}
     * @param listener the listener
     * @return the store itself
     */
    public NitmProxyListenerStore addBefore(Class<?> target, NitmProxyListener listener) {
        return addBefore(target, singleton(listener));
    }

    /**
     * Add the provider to the place before the {@code target}.
     *
     * @param target the target class, can be a subclass of {@link NitmProxyListener}
     *               or {@link NitmProxyListenerProvider}
     * @param provider the provider
     * @return the store itself
     */
    public NitmProxyListenerStore addBefore(Class<?> target, NitmProxyListenerProvider provider) {
        return addBefore(NitmProxyListenerProvider.match(target), provider);
    }

    /**
     * Add the listener to the place before the place that {@code predicate} was matched.
     *
     * @param predicate the predicate to determine the place for the listener to be inserted
     * @param listener the listener
     * @return the store itself
     */
    public NitmProxyListenerStore addBefore(Predicate<NitmProxyListenerProvider> predicate,
            NitmProxyListener listener) {
        return addBefore(predicate, singleton(listener));
    }

    /**
     * Add the provider to the place after the place that {@code predicate} was matched.
     *
     * @param predicate the predicate to determine the place for the listener to be inserted
     * @param provider the listener
     * @return the store itself
     */
    public NitmProxyListenerStore addBefore(Predicate<NitmProxyListenerProvider> predicate,
            NitmProxyListenerProvider provider) {
        int matched = IntStream.range(0, listeners.size())
                               .filter(index -> predicate.test(listeners.get(index)))
                               .findFirst()
                               .orElseThrow(NitmProxyException.toThrow("Listener not exist in store: %s", predicate));
        listeners.add(matched, provider);
        return this;
    }

    /**
     * Add the listener to the last place of the store.
     *
     * @param listener the listener
     * @return the store itself
     */
    public NitmProxyListenerStore addLast(NitmProxyListener listener) {
        return addLast(singleton(listener));
    }

    /**
     * Add the provider to the last place of the store.
     *
     * @param provider the listener
     * @return the store itself
     */
    public NitmProxyListenerStore addLast(NitmProxyListenerProvider provider) {
        listeners.add(provider);
        return this;
    }

    public List<NitmProxyListenerProvider> getListeners() {
        return listeners;
    }
}
