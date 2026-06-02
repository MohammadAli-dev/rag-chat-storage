package com.assessment.ragchat.session.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RenameSessionRequest {

    @NotBlank(message = "title is required")
    private String title;
}