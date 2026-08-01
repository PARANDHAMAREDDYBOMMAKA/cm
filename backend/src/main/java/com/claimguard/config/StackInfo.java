package com.claimguard.config;

import jakarta.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.SpringVersion;
import org.springframework.stereotype.Component;

@Component
public class StackInfo {

    private final ServletContext servletContext;
    private final String extraPoweredBy;

    public StackInfo(ServletContext servletContext,
            @Value("${STACK_EXTRA_POWERED_BY:}") String extraPoweredBy) {
        this.servletContext = servletContext;
        this.extraPoweredBy = extraPoweredBy;
    }

    public String server() {
        return "Apache-Coyote/1.1";
    }

    public String poweredBy() {
        String extra = extraPoweredBy.isBlank() ? "" : "; " + extraPoweredBy.trim();
        return "Servlet/" + servletVersion()
                + extra
                + "; Spring Boot " + SpringBootVersion.getVersion()
                + "; Spring " + SpringVersion.getVersion();
    }

    public String runtime() {
        return "Java/" + System.getProperty("java.version");
    }

    public String servletVersion() {
        return servletContext.getMajorVersion() + "." + servletContext.getMinorVersion();
    }

    public String tomcatVersion() {
        try {
            Class<?> info = Class.forName("org.apache.catalina.util.ServerInfo");
            return (String) info.getMethod("getServerNumber").invoke(null);
        } catch (ReflectiveOperationException exception) {
            return servletContext.getServerInfo();
        }
    }

    public String springBootVersion() {
        return SpringBootVersion.getVersion();
    }

    public String springVersion() {
        return SpringVersion.getVersion();
    }

    public String javaVersion() {
        return System.getProperty("java.version");
    }
}
