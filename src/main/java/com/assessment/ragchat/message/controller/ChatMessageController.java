package com.assessment.ragchat.message.controller;

import com.assessment.ragchat.message.dto.AddMessageRequest;
import com.assessment.ragchat.message.dto.AddMessageResponse;
import com.assessment.ragchat.message.dto.MessageResponse;
import com.assessment.ragchat.message.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService messageService;

    @PostMapping("/{id}/messages")
    public ResponseEntity<AddMessageResponse> addMessage(
            @PathVariable UUID id,
            @Valid @RequestBody AddMessageRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(messageService.addMessage(id, request));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                messageService.getMessages(id, PageRequest.of(page, size)));
    }
}