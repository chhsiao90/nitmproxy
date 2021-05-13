package com.github.chhsiao90.nitmproxy;

import org.junit.Test;

import static com.github.chhsiao90.nitmproxy.Address.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;

public class AddressTest {
    @Test
    public void shouldResolve() {
        assertEquals(address("localhost", 80), resolve("localhost:80"));
        assertEquals(address("localhost", 80), resolve("localhost", 80));
        assertEquals(address("www.google.com", 443), resolve("www.google.com:443"));
        assertEquals(address("www.google.com", 443), resolve("www.google.com", 443));
    }

    @Test
    public void shouldResolveFailed() {
        assertThatThrownBy(() -> resolve("localhost"));
    }
}
