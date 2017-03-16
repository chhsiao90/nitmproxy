package com.github.chhsiaoninety.nitmproxy.tls;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Provider;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class CertUtil {
    private static final Provider PROVIDER = new BouncyCastleProvider();

    public static Certificate newCert(String parentCertFile, String keyFile, String host) {
        try {
            Date before = Date.from(Instant.now());
            Date after = Date.from(Year.now().plus(3, ChronoUnit.YEARS).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            X509CertificateHolder parent = readPemFromFile(parentCertFile);
            PEMKeyPair pemKeyPair = readPemFromFile(keyFile);
            KeyPair keyPair = new JcaPEMKeyConverter()
                    .setProvider(PROVIDER)
                    .getKeyPair(pemKeyPair);

            X509v3CertificateBuilder x509 = new JcaX509v3CertificateBuilder(
                    parent.getSubject(),
                    new BigInteger(64, new SecureRandom()),
                    before,
                    after,
                    new X500Name("CN=" + host),
                    keyPair.getPublic());

            ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                    .build(keyPair.getPrivate());

            JcaX509CertificateConverter x509CertificateConverter = new JcaX509CertificateConverter()
                    .setProvider(PROVIDER);

            return new Certificate(
                    keyPair,
                    x509CertificateConverter.getCertificate(x509.build(signer)),
                    x509CertificateConverter.getCertificate(parent));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> T readPemFromFile(String pemFile) throws IOException {
        try (PEMParser pemParser = new PEMParser(new FileReader(pemFile))) {
            Object o = pemParser.readObject();

            @SuppressWarnings("unchecked")
            T t = (T) o;
            return t;
        }
    }

    @SuppressWarnings("deprecation")
    public static byte[] toPem(Object object) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PEMWriter writer = new PEMWriter(new OutputStreamWriter(outputStream))) {
            writer.writeObject(object);
            writer.flush();
            return outputStream.toByteArray();
        }
    }

    @SuppressWarnings("deprecation")
    public static byte[] toPem(Object... objects) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PEMWriter writer = new PEMWriter(new OutputStreamWriter(outputStream))) {
            for (Object object : objects) {
                writer.writeObject(object);
            }
            writer.flush();
            return outputStream.toByteArray();
        }
    }
}
