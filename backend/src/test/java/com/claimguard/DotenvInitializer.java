package com.claimguard;

import com.claimguard.support.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

public class DotenvInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("dotenv", Dotenv.properties()));
    }
}
