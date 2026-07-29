package com.claimguard.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.SpringVersion;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "app.expose-tech", havingValue = "true")
public class TechDisclosureFilter extends OncePerRequestFilter {

    private final String server;
    private final String poweredBy;
    private final String runtime;

    public TechDisclosureFilter() {
        this.server = "Apache Tomcat/" + tomcatVersion();
        this.poweredBy = "Servlet/6.1; Spring Boot " + SpringBootVersion.getVersion()
                + "; Spring " + SpringVersion.getVersion();
        this.runtime = "Java/" + System.getProperty("java.version");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Server", server);
        response.setHeader("X-Powered-By", poweredBy);
        response.setHeader("X-Runtime", runtime);
        chain.doFilter(request, response);
    }

    private static String tomcatVersion() {
        try {
            Class<?> info = Class.forName("org.apache.catalina.util.ServerInfo");
            return (String) info.getMethod("getServerNumber").invoke(null);
        } catch (ReflectiveOperationException exception) {
            return "11.0";
        }
    }
}
