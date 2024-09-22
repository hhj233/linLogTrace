package com.lin.project;

import org.slf4j.LogTraceTtlAdapter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author linzj
 */
public class LogTraceTtlAdapterInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        LogTraceTtlAdapter.getInstance();
    }
}
