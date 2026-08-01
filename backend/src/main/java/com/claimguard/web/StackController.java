package com.claimguard.web;

import com.claimguard.config.StackInfo;
import com.claimguard.web.dto.StackResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stack")
public class StackController {

    private final StackInfo stack;

    public StackController(StackInfo stack) {
        this.stack = stack;
    }

    @GetMapping
    public StackResponse stack() {
        return new StackResponse(
                stack.server(),
                stack.poweredBy(),
                stack.runtime(),
                stack.javaVersion(),
                stack.tomcatVersion(),
                stack.servletVersion(),
                stack.springBootVersion(),
                stack.springVersion());
    }
}
