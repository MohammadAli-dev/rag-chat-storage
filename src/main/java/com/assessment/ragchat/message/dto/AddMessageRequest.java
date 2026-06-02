package com.assessment.ragchat.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AddMessageRequest {

    @NotBlank(message = "content is required")
    private String content;

    @NotNull(message = "sender is required")
    private SenderType sender;

    private List<ContextChunk> context;
}