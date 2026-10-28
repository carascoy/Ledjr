package com.Ledjr.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * makes request.getRemoteAddr() the user's real IP when we're behind Heroku's router.
 *
 * the problem: on Heroku nobody connects to the app directly. the router does, so every request
 * looks like it came from the router. the login limiter counts wrong passwords per IP, so it would
 * see ONE ip for the whole internet, and 20 bad logins from anyone would lock out everyone.
 *
 * the router writes the real IP into the X-Forwarded-For header. but a user can send their own
 * X-Forwarded-For too ("X-Forwarded-For: 1.2.3.4") to pretend to be someone else, and the router
 * keeps it and adds the real one at the END: "1.2.3.4, <real ip>". so only the LAST one is trustworthy.
 * (Spring's built-in ForwardedHeaderFilter takes the FIRST one, which is the fakeable one)
 *
 * runs first so everything after it, including Spring Security, sees the real IP.
 * only on when app.trust-proxy=true; locally there's no router and the header could be anything
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ClientIpFilter extends OncePerRequestFilter {

    private final boolean trustProxy;

    public ClientIpFilter(@Value("${app.trust-proxy}") boolean trustProxy){
        this.trustProxy = trustProxy;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String realIp = trustProxy ? lastForwardedIp(request.getHeader("X-Forwarded-For")) : null;
        if (realIp == null) {
            chain.doFilter(request, response);
            return;
        }
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String getRemoteAddr(){
                return realIp;
            }
        }, response);
    }

    /** "1.2.3.4, 5.6.7.8" -> "5.6.7.8". null if there's no header or it's empty */
    static String lastForwardedIp(String header){
        if (header == null) return null;
        String[] parts = header.split(",");
        String last = parts[parts.length - 1].trim();
        return last.isEmpty() ? null : last;
    }
}
