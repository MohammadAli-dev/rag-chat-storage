package com.assessment.ragchat.session.controller;

import com.assessment.ragchat.session.dto.*;
import com.assessment.ragchat.session.service.ChatSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService sessionService;

    @PostMapping
    public ResponseEntity<CreateSessionResponse> createSession(
            @Valid @RequestBody CreateSessionRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(sessionService.createSession(request));
    }

    @GetMapping
    public ResponseEntity<List<SessionResponse>> listSessions(
            @RequestParam String userId) {
        return ResponseEntity.ok(sessionService.listSessions(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSession(
            @PathVariable UUID id) {
        return ResponseEntity.ok(sessionService.getSession(id));
    }

    @PatchMapping("/{id}/rename")
    public ResponseEntity<SessionResponse> renameSession(
            @PathVariable UUID id,
            @Valid @RequestBody RenameSessionRequest request) {
        return ResponseEntity.ok(sessionService.renameSession(id, request));
    }

    @PatchMapping("/{id}/favorite")
    public ResponseEntity<SessionResponse> toggleFavorite(
            @PathVariable UUID id) {
        return ResponseEntity.ok(sessionService.toggleFavorite(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable UUID id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}