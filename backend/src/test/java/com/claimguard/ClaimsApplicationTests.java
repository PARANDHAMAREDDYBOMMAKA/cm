package com.claimguard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = DotenvInitializer.class)
class ClaimsApplicationTests {

    @Test
    void contextLoads() {
    }
}
