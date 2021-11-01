package com.github.chhsiao90.nitmproxy.testing;

import com.github.chhsiao90.nitmproxy.listener.NitmProxyListenerProvider;
import com.github.chhsiao90.nitmproxy.listener.NitmProxyListenerStore;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ListAssert;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

public class NitmProxyListenerStoreAssert extends AbstractAssert<NitmProxyListenerStoreAssert, NitmProxyListenerStore> {
    public NitmProxyListenerStoreAssert(NitmProxyListenerStore actual) {
        super(actual, NitmProxyListenerStoreAssert.class);
    }

    @SafeVarargs
    public final NitmProxyListenerStoreAssert hasListeners(Condition<NitmProxyListenerProvider>... conditions) {
        assertThat(actual.getListeners()).hasSize(conditions.length);
        IntStream.range(0, conditions.length)
                 .forEach(index -> assertThat(actual.getListeners()).has(conditions[index], atIndex(index)));
        return this;
    }
}
