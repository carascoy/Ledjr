package com.Ledjr.config;

import static com.Ledjr.Fakes.fake;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** the login limiter counts per IP, so the IP it sees has to be the real one and not fakeable */
class ClientIpFilterTest {

    private static final String ROUTER = "10.1.2.3";

    /** what getRemoteAddr() looks like to the code after the filter */
    private String ipSeenBehind(boolean trustProxy, String forwardedFor) throws Exception {
        HttpServletRequest request = fake(HttpServletRequest.class, Map.of(
            "getRemoteAddr", args -> ROUTER,
            "getHeader", args -> "X-Forwarded-For".equals(args[0]) ? forwardedFor : null));
        AtomicReference<String> seen = new AtomicReference<>();
        FilterChain chain = (req, res) -> seen.set(req.getRemoteAddr());
        new ClientIpFilter(trustProxy).doFilterInternal(request, fake(HttpServletResponse.class, Map.of()), chain);
        return seen.get();
    }

    @Test
    void behindTheRouterUsesTheRealIp() throws Exception {
        assertEquals("203.0.113.7", ipSeenBehind(true, "203.0.113.7"));
    }

    @Test
    void aFakedHeaderDoesntWorkBecauseTheRouterAddsTheRealIpLast() throws Exception {
        // someone sends "X-Forwarded-For: 1.1.1.1" hoping to dodge the limit, the router adds theirs after
        assertEquals("203.0.113.7", ipSeenBehind(true, "1.1.1.1, 203.0.113.7"));
    }

    @Test
    void notBehindARouterTheHeaderIsIgnored() throws Exception {
        // locally anyone can send the header, so it means nothing
        assertEquals(ROUTER, ipSeenBehind(false, "1.1.1.1"));
    }

    @Test
    void noHeaderKeepsTheConnectionIp() throws Exception {
        assertEquals(ROUTER, ipSeenBehind(true, null));
    }

    @Test
    void readsTheLastEntry() {
        assertEquals("5.6.7.8", ClientIpFilter.lastForwardedIp("1.2.3.4,5.6.7.8"));
        assertEquals("5.6.7.8", ClientIpFilter.lastForwardedIp(" 5.6.7.8 "));
        assertNull(ClientIpFilter.lastForwardedIp(""));
        assertNull(ClientIpFilter.lastForwardedIp("1.2.3.4, "));
        assertNull(ClientIpFilter.lastForwardedIp(null));
    }
}
