package com.therighthandapp.autobads.ingestion;

import com.therighthandapp.autobads.core.domain.BusinessIdea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BusinessIdeaRepository extends JpaRepository<BusinessIdea, UUID> {
}

