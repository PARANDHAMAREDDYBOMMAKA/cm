package com.claimguard.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "app.expose-tech", havingValue = "true")
public class TechDisclosureFilter extends OncePerRequestFilter {

    private final StackInfo stack;

    public TechDisclosureFilter(StackInfo stack) {
        this.stack = stack;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Server", stack.server());
        response.setHeader("X-Powered-By", stack.poweredBy());
        response.setHeader("X-Runtime", stack.runtime());
        chain.doFilter(request, response);
    }
}
