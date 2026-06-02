package com.assessment.ragchat.message.entity;

import com.assessment.ragchat.message.dto.ContextChunk;
import com.assessment.ragchat.message.dto.SenderType;
import com.assessment.ragchat.session.entity.ChatSession;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SenderType sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Convert(converter = ContextChunkConverter.class)
    @Column(columnDefinition = "jsonb")
    private List<ContextChunk> context;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}