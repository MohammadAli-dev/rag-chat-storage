package com.assessment.ragchat.message.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AddMessageResponse {

    private UUID id;
    private UUID sessionId;
    private SenderType sender;
    private String content;
    private List<ContextChunk> context;
    private LocalDateTime createdAt;
}