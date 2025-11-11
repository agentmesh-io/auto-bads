package com.therighthandapp.autobads.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import com.therighthandapp.autobads.ingestion.BusinessIdeaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom health indicator for database connectivity and basic operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseHealthIndicator implements HealthIndicator {

    private final BusinessIdeaRepository repository;

    @Override
    public Health health() {
        try {
            // Simple count query to test database connectivity
            long count = repository.count();
            
            return Health.up()
                    .withDetail("database", "operational")
                    .withDetail("total_ideas", count)
                    .build();
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Health.down()
                    .withDetail("database", "unavailable")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
