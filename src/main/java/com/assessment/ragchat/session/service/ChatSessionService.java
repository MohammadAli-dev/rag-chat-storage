package com.assessment.ragchat.session.service;

import com.assessment.ragchat.exception.SessionNotFoundException;
import com.assessment.ragchat.session.dto.*;
import com.assessment.ragchat.session.entity.ChatSession;
import com.assessment.ragchat.session.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository sessionRepository;

    @Transactional
    public CreateSessionResponse createSession(CreateSessionRequest request) {
        ChatSession session = ChatSession.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .favorite(false)
                .build();

        ChatSession saved = sessionRepository.save(session);
        return toCreateResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions(String userId) {
        return sessionRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(UUID id) {
        ChatSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new SessionNotFoundException(id));
        return toSessionResponse(session);
    }

    @Transactional
    public SessionResponse renameSession(UUID id, RenameSessionRequest request) {
        ChatSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new SessionNotFoundException(id));

        session.setTitle(request.getTitle());
        ChatSession saved = sessionRepository.save(session);
        return toSessionResponse(saved);
    }

    @Transactional
    public SessionResponse toggleFavorite(UUID id) {
        ChatSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new SessionNotFoundException(id));

        session.setFavorite(!session.isFavorite());
        ChatSession saved = sessionRepository.save(session);
        return toSessionResponse(saved);
    }

    @Transactional
    public void deleteSession(UUID id) {
        if (!sessionRepository.existsById(id)) {
            throw new SessionNotFoundException(id);
        }
        sessionRepository.deleteById(id);
    }

    // ---- Mappers ----

    private CreateSessionResponse toCreateResponse(ChatSession session) {
        return CreateSessionResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .title(session.getTitle())
                .favorite(session.isFavorite())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private SessionResponse toSessionResponse(ChatSession session) {
        return SessionResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .title(session.getTitle())
                .favorite(session.isFavorite())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}