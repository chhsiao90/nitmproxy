package com.github.chhsiaoninety.nitmproxy.handler.protocol.http2;

import io.netty.channel.Channel;
import io.netty.handler.codec.http2.Http2Exception;

public class Http2TestUtil {

  private Http2TestUtil() {
  }

  /**
   * Interface that allows for running a operation that throws a {@link Http2Exception}.
   */
  interface Http2Runnable {
    void run() throws Http2Exception;
  }

  /**
   * Runs the given operation within the event loop thread of the given {@link Channel}.
   */
  static void runInChannel(Channel channel, final Http2Runnable runnable) {
    channel.eventLoop().execute(() -> {
      try {
        runnable.run();
      } catch (Http2Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
