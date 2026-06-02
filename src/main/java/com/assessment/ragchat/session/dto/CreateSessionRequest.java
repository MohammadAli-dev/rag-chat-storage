package com.assessment.ragchat.session.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSessionRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "title is required")
    private String title;
}