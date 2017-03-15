package com.github.chhsiaoninety.nitm.nitmproxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
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

public class NitmProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(NitmProxy.class);

    private NitmProxyConfig config;

    public NitmProxy(NitmProxyConfig config) {
        this.config = config;
    }

    public void start() throws Exception {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new NitmProxyInitializer(config));
            Channel channel = bootstrap
                    .bind(config.getHost(), config.getPort())
                    .sync()
                    .channel();

            System.err.format("nitmproxy is listened at http://%s:%d%n",
                              config.getHost(), config.getPort());

            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();

        options.addOption(
                Option.builder("m")
                      .longOpt("mode")
                      .hasArg()
                      .argName("MODE")
                      .desc("proxy mode(HTTP, SOCKS), default: HTTP")
                      .build());
        options.addOption(
                Option.builder("h")
                      .longOpt("host")
                      .hasArg()
                      .argName("HOST")
                      .desc("listening host, default: 127.0.0.1")
                      .build());
        options.addOption(
                Option.builder("p")
                      .longOpt("port")
                      .hasArg()
                      .argName("PORT")
                      .desc("listening port, default: 8080")
                      .build());
        options.addOption(
                Option.builder()
                      .longOpt("cert")
                      .hasArg()
                      .argName("CERTIFICATE")
                      .desc("x509 certificate used by server(*.pem), default: server.pem")
                      .build());
        options.addOption(
                Option.builder()
                      .longOpt("key")
                      .hasArg()
                      .argName("KEY")
                      .desc("key used by server(*.pem), default: key.pem")
                      .build());
        options.addOption(
                Option.builder()
                      .longOpt("clientNoHttp2")
                      .hasArg(false)
                      .desc("disable http2 for client")
                      .build());
        options.addOption(
                Option.builder()
                      .longOpt("serverNoHttp2")
                      .hasArg(false)
                      .desc("disable http2 for server")
                      .build());
        options.addOption(
                Option.builder("k")
                      .longOpt("insecure")
                      .hasArg(false)
                      .desc("not verify on server certificate")
                      .build());

        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            new HelpFormatter().printHelp("nitmproxy", options, true);
            System.exit(-1);
        }

        new NitmProxy(parse(commandLine)).start();
    }

    private static NitmProxyConfig parse(CommandLine commandLine) {
        NitmProxyConfig config = new NitmProxyConfig();
        if (commandLine.hasOption("m")) {
            config.setProxyMode(ProxyMode.of(commandLine.getOptionValue("m")));
        }
        if (commandLine.hasOption("h")) {
            config.setHost(commandLine.getOptionValue("h"));
        }
        if (commandLine.hasOption("p")) {
            try {
                config.setPort(Integer.parseInt(commandLine.getOptionValue("p")));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Not a legal port: " + commandLine.getOptionValue("p"));
            }
        }
        if (commandLine.hasOption("cert")) {
            String certFile = commandLine.getOptionValue("cert");
            if (!new File(certFile).exists()) {
                throw new IllegalArgumentException("No cert file found: " + certFile);
            }
            config.setCertFile(certFile);
        }
        if (commandLine.hasOption("key")) {
            String certKey = commandLine.getOptionValue("key");
            if (!new File(certKey).exists()) {
                throw new IllegalArgumentException("No key found: " + certKey);
            }
            config.setKeyFile(certKey);
        }
        if (commandLine.hasOption("clientNoHttp2")) {
            config.setClientHttp2(false);
        }
        if (commandLine.hasOption("serverNoHttp2")) {
            config.setServerHttp2(false);
        }
        if (commandLine.hasOption("k")) {
            config.setInsecure(true);
        }

        LOGGER.info("{}", config);
        return config;
    }
}