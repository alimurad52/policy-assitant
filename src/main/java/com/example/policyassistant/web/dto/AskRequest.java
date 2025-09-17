package com.example.policyassistant.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AskRequest(
        @NotBlank(message = "Question is required")
        String question,
        List<String> fileIds,
        String chatId
) {
}
