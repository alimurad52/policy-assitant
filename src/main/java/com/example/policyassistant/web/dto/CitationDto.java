package com.example.policyassistant.web.dto;

public record CitationDto(
        String fileId,
        String fileName,
        String text,
        int pageNumber,
        int lineNumber
) {
}
