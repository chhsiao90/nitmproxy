package com.github.chhsiao90.nitmproxy.tls;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.CertIOException;
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
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class CertUtil {

    private static final Provider PROVIDER = new BouncyCastleProvider();

    private CertUtil() {
    }

    public static Certificate newCert(String parentCertFile, String keyFile, String host) {
        try {
            //need a date before today to adjust for other time zones
            Date before = Date.from(
                    Instant.now()
                    .atZone(ZoneId.systemDefault())
                    .minusMonths(6).toInstant());

            Date after = Date.from(
                    Year.now()
                        .plus(1, ChronoUnit.YEARS)
                        .atDay(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant());

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
            GeneralNames generalNames = GeneralNames.getInstance(
                    new DERSequence(new GeneralName(GeneralName.dNSName, host)));
            x509.addExtension(Extension.subjectAlternativeName, true, generalNames);

            //add extended key usage needed for newer Mac OS requirements
            x509.addExtension(
                    Extension.extendedKeyUsage,
                    true,
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));

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

    /**
     * Generate a key pair for private and public key generation
     *
     * @param keySize
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(keySize, new SecureRandom());
        return keyGen.generateKeyPair();
    }


    /**
     * Generate a server certificate
     *
     *
     * @param subject
     * @param beforeDate
     * @param notAfterDate
     * @param keyPair
     * @return
     * @throws CertIOException
     * @throws OperatorCreationException
     * @throws CertificateException
     */
    public static X509Certificate generateCertificate(String subject, Date beforeDate, Date notAfterDate,
                                                      KeyPair keyPair)
            throws CertIOException, OperatorCreationException, CertificateException {

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(new X500Name(subject),
                new BigInteger(Long.toString(System.currentTimeMillis() / 1000)),
                beforeDate,
                notAfterDate,
                new X500Name(subject),
                keyPair.getPublic());
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer));
    }

    /**
     * Used as a helper for creating server certificates which do not depend on openssl being installed. It
     * does overwrite existing certificates if they are present.
     *
     * @param caCertFile
     * @param caPrivateFile
     * @param keySize
     * @throws Exception
     */
    public static void createCACertificates(File caCertFile, File caPrivateFile, String subject, int keySize)
            throws Exception {

        KeyPair keyPair = generateKeyPair(keySize);
        if (caCertFile.exists()) {
            caCertFile.delete();
        }

        //need a date before today to adjust for other time zones
        Date before = Date.from(
                Instant.now()
                        .atZone(ZoneId.systemDefault())
                        .minusMonths(6).toInstant());

        Date after = Date.from(
                Year.now()
                        .plus(1, ChronoUnit.YEARS)
                        .atDay(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant());

        Files.write(Paths.get(caCertFile.toURI()),
                toPem(generateCertificate(
                        subject,
                        before,
                        after,
                        keyPair)));

        if (caPrivateFile.exists()) {
            caPrivateFile.delete();
        }

        Files.write(caPrivateFile.toPath(),
                toPem(keyPair.getPrivate()));
    }
}
