package com.assessment.ragchat.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextChunk {

    private String chunkId;
    private String content;
    private String sourceUrl;
    private Double score;
}