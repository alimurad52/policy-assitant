package com.example.policyassistant.web.dto;

import java.time.Instant;

public record ChatResponse(
        String id,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
}
