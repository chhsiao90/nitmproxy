package com.github.chhsiao90.nitmproxy.tls;

import com.github.chhsiao90.nitmproxy.NitmProxyConfig;
import com.github.chhsiao90.nitmproxy.exception.NitmProxyException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class CertManager {

    private static final int CERT_CACHE_SIZE = 2000;

    private final String certFile;
    private final String keyFile;

    private LoadingCache<String, Certificate> certsCache;

    public CertManager(NitmProxyConfig config) {
        this.certFile = new File(config.getCertFile()).getAbsolutePath();
        this.keyFile = new File(config.getKeyFile()).getAbsolutePath();
        this.certsCache = CacheBuilder
                .newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .maximumSize(CERT_CACHE_SIZE)
                .build(new CacheLoader<String, Certificate>() {
                    @Override
                    public Certificate load(String host) {
                        return createCert(host);
                    }
                });
    }

    public Certificate getCert(String host) {
        try {
            return certsCache.get(host);
        } catch (ExecutionException e) {
            throw new NitmProxyException("Create cert failed", e.getCause());
        }
    }

    private Certificate createCert(String host) {
        return CertUtil.newCert(certFile, keyFile, host);
    }
}
