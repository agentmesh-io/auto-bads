package com.therighthandapp.autobads;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-BADS Application - Autonomous Business Analysis and Development Service
 *
 * A compound AI system featuring multi-agent collaboration with hybrid LLM/DL architecture.
 * Provides autonomous analysis of business ideas and generates actionable solution blueprints.
 */
@Modulith(
        systemName = "Auto-BADS",
        sharedModules = "core"
)
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class AutoBadsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoBadsApplication.class, args);
    }
}

