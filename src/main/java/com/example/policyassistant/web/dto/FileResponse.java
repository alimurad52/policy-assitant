package com.example.policyassistant.web.dto;

import java.time.Instant;

public record FileResponse(
        String id,
        String name,
        long size,
        Instant uploadedAt
) {
}
