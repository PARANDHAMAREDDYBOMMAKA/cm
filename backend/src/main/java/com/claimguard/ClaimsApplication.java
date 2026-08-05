package com.claimguard;

import com.claimguard.support.Dotenv;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClaimsApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ClaimsApplication.class)
                .properties(Dotenv.properties())
                .run(args);
    }
}
