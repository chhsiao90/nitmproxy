package com.github.chhsiao90.nitmproxy;

import com.github.chhsiao90.nitmproxy.tls.CertUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class CertGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CertGenerator.class);

    private static final String DEFAULT_SUBJECT = "C=US, ST=VA, L=Vienna, O=Nitm, OU=Nitm, CN=Nitm CA Root";
    private static final int DEFAULT_KEYSIZE = 2048;

    private CertGenerator() {
    }

    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();

        options.addOption(
                Option.builder("s")
                      .longOpt("subject")
                      .hasArg()
                      .argName("SUBJECT")
                      .desc("subject of certificate, default: " + DEFAULT_SUBJECT)
                      .build());
        options.addOption(
                Option.builder("k")
                        .longOpt("keysize")
                        .hasArg()
                        .argName("KEYSIZE")
                        .desc("key size of certificate, default: 2048")
                        .build());

        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            new HelpFormatter().printHelp("certgenerator", options, true);
            System.exit(-1);
        }

        CertGeneratorConfig config = parse(commandLine);
        LOGGER.info("Generating certificate with subject:{} and keysize:{}",
                config.getSubject(), config.getKeySize());

        File serverPem = new File("server.pem");
        File keyPem = new File("key.pem");

        CertUtil.createCACertificates(serverPem, keyPem, config.getSubject(), config.getKeySize());

        //we'll copy server.pem to server.crt for easy import
        Files.copy(Paths.get(serverPem.toURI()), Paths.get("server.crt"));
    }

    private static CertGeneratorConfig parse(CommandLine commandLine) {
        CertGeneratorConfig config = new CertGeneratorConfig();
        if (commandLine.hasOption("s")) {
            config.setSubject(commandLine.getOptionValue("s"));
        }
        if (commandLine.hasOption("k")) {
            try {
                config.setKeySize(Integer.parseInt(commandLine.getOptionValue("k")));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Not a valid key size: " + commandLine.getOptionValue("k"));
            }
        }
        return config;
    }

    private static class CertGeneratorConfig {
        String subject = DEFAULT_SUBJECT;
        int keySize = DEFAULT_KEYSIZE;

        public int getKeySize() {
            return keySize;
        }

        public void setKeySize(int keySize) {
            this.keySize = keySize;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }
    }
}