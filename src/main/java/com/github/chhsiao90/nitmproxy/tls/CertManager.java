package com.github.chhsiao90.nitmproxy.tls;

import com.github.chhsiao90.nitmproxy.NitmProxyConfig;
import com.github.chhsiao90.nitmproxy.exception.NitmProxyException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;

import java.security.PrivateKey;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.*;

public class CertManager {

    private static final int CERT_CACHE_SIZE = 2000;

    private final X509CertificateHolder certificate;
    private final PrivateKeyInfo key;

    private LoadingCache<String, Certificate> certsCache;

    public CertManager(NitmProxyConfig config) {
        this.certificate = checkNotNull(config.getCertificate(), "certificate");
        this.key = checkNotNull(config.getKey(), "key");
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
        return CertUtil.newCert(certificate, key, host);
    }
}
