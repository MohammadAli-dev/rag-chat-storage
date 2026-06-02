package com.assessment.ragchat.message.service;

import com.assessment.ragchat.exception.SessionNotFoundException;
import com.assessment.ragchat.message.dto.AddMessageRequest;
import com.assessment.ragchat.message.dto.AddMessageResponse;
import com.assessment.ragchat.message.dto.MessageResponse;
import com.assessment.ragchat.message.entity.ChatMessage;
import com.assessment.ragchat.message.repository.ChatMessageRepository;
import com.assessment.ragchat.session.entity.ChatSession;
import com.assessment.ragchat.session.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository messageRepository;
    private final ChatSessionRepository sessionRepository;

    @Transactional
    public AddMessageResponse addMessage(UUID sessionId, AddMessageRequest request) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        ChatMessage message = ChatMessage.builder()
                .session(session)
                .sender(request.getSender())
                .content(request.getContent())
                .context(request.getContext())
                .build();

        ChatMessage saved = messageRepository.save(message);
        return toAddMessageResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(UUID sessionId, Pageable pageable) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new SessionNotFoundException(sessionId);
        }
        return messageRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId, pageable)
                .map(this::toMessageResponse);
    }

    // ---- Mappers ----

    private AddMessageResponse toAddMessageResponse(ChatMessage message) {
        return AddMessageResponse.builder()
                .id(message.getId())
                .sessionId(message.getSession().getId())
                .sender(message.getSender())
                .content(message.getContent())
                .context(message.getContext())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private MessageResponse toMessageResponse(ChatMessage message) {
        return MessageResponse.builder()
                .id(message.getId())
                .sessionId(message.getSession().getId())
                .sender(message.getSender())
                .content(message.getContent())
                .context(message.getContext())
                .createdAt(message.getCreatedAt())
                .build();
    }
}