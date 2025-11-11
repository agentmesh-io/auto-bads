package com.therighthandapp.autobads.core.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Core domain entity representing a business idea submitted for analysis
 */
@Entity
@Table(name = "business_ideas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessIdea {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String title;
    private String description;
    private String rawIdea;  // Original unprocessed idea text
    private String structuredProblemStatement;  // AI-generated structured statement
    private String submittedBy;
    
    @Column(name = "submitted_at")
    private Instant submittedAt;
    
    private String industry;
    
    @Column(name = "target_market")
    private String targetMarket;
    
    @ElementCollection
    @CollectionTable(name = "business_idea_metadata", joinColumns = @JoinColumn(name = "idea_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value", length = 1000)
    private Map<String, String> metadata;
    
    @Enumerated(EnumType.STRING)
    private Status status;
    
    /**
     * Add metadata key-value pair
     */
    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
    
    /**
     * Get metadata value by key
     */
    public String getMetadataValue(String key) {
        return this.metadata != null ? this.metadata.get(key) : null;
    }
}
