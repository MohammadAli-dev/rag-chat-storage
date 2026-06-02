package com.assessment.ragchat.session.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SessionResponse {

    private UUID id;
    private String userId;
    private String title;
    private boolean favorite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}