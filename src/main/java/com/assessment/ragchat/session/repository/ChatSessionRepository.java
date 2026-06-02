package com.assessment.ragchat.session.repository;

import com.assessment.ragchat.session.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    List<ChatSession> findByUserIdOrderByCreatedAtDesc(String userId);

    boolean existsByIdAndUserId(UUID id, String userId);
}