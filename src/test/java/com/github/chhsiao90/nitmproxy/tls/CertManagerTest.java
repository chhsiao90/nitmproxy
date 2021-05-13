package com.github.chhsiao90.nitmproxy.tls;

import com.github.chhsiao90.nitmproxy.NitmProxyConfig;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;

import static com.github.chhsiao90.nitmproxy.tls.CertUtil.*;
import static org.junit.Assert.*;

public class CertManagerTest {
    private CertManager certManager;

    @Before
    public void setUp() {
        NitmProxyConfig config = new NitmProxyConfig();
        config.setCertificate(readPemFromFile(Resources.getResource("server.pem").getFile()));
        config.setKey(readPrivateKeyFromFile(Resources.getResource("key.pem").getFile()));
        certManager = new CertManager(config);
    }

    @Test
    public void shouldCreateCert() {
        assertNotNull(certManager.getCert("localhost"));
    }

    @Test
    public void shouldCacheCert() {
        assertSame(
                certManager.getCert("localhost"),
                certManager.getCert("localhost"));
        assertNotSame(
                certManager.getCert("www.google.com"),
                certManager.getCert("www.apple.com"));
    }
}
