package com.example.policyassistant.web.dto;

import java.util.List;

public record AskResponse(
        String chatId,
        String chatTitle,
        String answer,
        List<CitationDto> citations
) {
}
