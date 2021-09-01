package com.github.chhsiao90.nitmproxy.tls;

import com.github.chhsiao90.nitmproxy.Address;
import com.github.chhsiao90.nitmproxy.ConnectionContext;
import com.github.chhsiao90.nitmproxy.tls.SimpleUnsafeAccessSupport.Interceptor;
import io.netty.handler.codec.http.FullHttpRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static com.github.chhsiao90.nitmproxy.http.HttpUtil.*;
import static com.github.chhsiao90.nitmproxy.testing.ResponseAssert.*;
import static com.github.chhsiao90.nitmproxy.tls.UnsafeAccess.*;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SimpleUnsafeAccessSupportTest {
    private SimpleUnsafeAccessSupport unsafeAccessSupport;
    private Interceptor interceptor;
    private ConnectionContext context;
    private FullHttpRequest request;

    @Before
    public void setUp() throws IOException {
        context = mock(ConnectionContext.class);
        when(context.getServerAddr()).thenReturn(new Address("www.google.com", 443));

        unsafeAccessSupport = new SimpleUnsafeAccessSupport();
        interceptor = (Interceptor) unsafeAccessSupport.getInterceptor();
        request = request(HTTP_1_1, GET, "www.google.com", "/");
    }

    @After
    public void tearDown() {
        request.release();
    }

    @Test
    public void shouldGetAsk() {
        assertEquals(ASK, unsafeAccessSupport.checkUnsafeAccess(context, new X509Certificate[0], new CertificateException()));
    }

    @Test
    public void shouldNotIntercept() {
        assertThat(interceptor.onHttp1Request(context, request)).isEmpty();
    }

    @Test
    public void shouldInterceptOnAsk() {
        assertEquals(ASK, unsafeAccessSupport.checkUnsafeAccess(context, new X509Certificate[0], new CertificateException()));

        assertThat(interceptor.onHttp1Request(context, request))
                .isPresent()
                .get(asResponse())
                .content()
                .hasContent("<html>\n" +
                            "<body>\n" +
                            "  <a href=\"/;nitmproxy-unsafe=accept\">Accept</a>\n" +
                            "  <a href=\"/;nitmproxy-unsafe=deny\">Reject</a>\n" +
                            "</body>\n" +
                            "</html>")
                .release();
    }

    @Test
    public void shouldInterceptOnAccept() {
        assertEquals(ASK, unsafeAccessSupport.checkUnsafeAccess(context, new X509Certificate[0], new CertificateException()));

        request.setUri("/;nitmproxy-unsafe=accept");
        assertThat(interceptor.onHttp1Request(context, request)).isEmpty();
        assertEquals("/", request.uri());

        assertEquals(ACCEPT, unsafeAccessSupport.checkUnsafeAccess(context, new X509Certificate[0], new CertificateException()));
    }

    @Test
    public void shouldInterceptOnDeny() {
        assertEquals(ASK, unsafeAccessSupport.checkUnsafeAccess(context, new X509Certificate[0], new CertificateException()));

        request.setUri("/;nitmproxy-unsafe=deny");
        assertThat(interceptor.onHttp1Request(context, request))
                .isPresent()
                .get(asResponse())
                .status(FORBIDDEN)
                .release();
        assertEquals(DENY, unsafeAccessSupport.checkUnsafeAccess(context, new X509Certificate[0], new CertificateException()));

        request.setUri("/");
        assertThat(interceptor.onHttp1Request(context, request))
                .isPresent()
                .get(asResponse())
                .status(FORBIDDEN)
                .release();
    }
}
