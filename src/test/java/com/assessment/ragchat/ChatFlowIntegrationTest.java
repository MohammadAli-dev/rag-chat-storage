package com.assessment.ragchat;

import com.assessment.ragchat.message.dto.AddMessageRequest;
import com.assessment.ragchat.message.dto.SenderType;
import com.assessment.ragchat.session.dto.CreateSessionRequest;
import com.assessment.ragchat.session.dto.RenameSessionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private static final String API_KEY = "test-api-key";
    private static final String API_KEY_HEADER = "X-API-KEY";
    private static UUID sessionId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @Order(1)
    void shouldCreateSession() throws Exception {
        CreateSessionRequest request = new CreateSessionRequest();
        request.setUserId("integration-user");
        request.setTitle("Integration Test Session");

        MvcResult result = mockMvc.perform(post("/api/v1/sessions")
                        .header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value("integration-user"))
                .andExpect(jsonPath("$.title").value("Integration Test Session"))
                .andExpect(jsonPath("$.favorite").value(false))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String id = objectMapper.readTree(responseBody).get("id").asText();
        sessionId = UUID.fromString(id);
    }

    @Test
    @Order(2)
    void shouldAddMessage() throws Exception {
        AddMessageRequest request = new AddMessageRequest();
        request.setSender(SenderType.USER);
        request.setContent("What is the capital of France?");

        mockMvc.perform(post("/api/v1/sessions/{id}/messages", sessionId)
                        .header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.sender").value("USER"))
                .andExpect(jsonPath("$.content").value("What is the capital of France?"));
    }

    @Test
    @Order(3)
    void shouldAddBotMessage() throws Exception {
        AddMessageRequest request = new AddMessageRequest();
        request.setSender(SenderType.BOT);
        request.setContent("The capital of France is Paris.");

        mockMvc.perform(post("/api/v1/sessions/{id}/messages", sessionId)
                        .header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sender").value("BOT"))
                .andExpect(jsonPath("$.content").value("The capital of France is Paris."));
    }

    @Test
    @Order(4)
    void shouldGetMessages() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/{id}/messages", sessionId)
                        .header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    @Order(5)
    void shouldRenameSession() throws Exception {
        RenameSessionRequest request = new RenameSessionRequest();
        request.setTitle("Renamed Session");

        mockMvc.perform(patch("/api/v1/sessions/{id}/rename", sessionId)
                        .header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed Session"));
    }

    @Test
    @Order(6)
    void shouldToggleFavorite() throws Exception {
        mockMvc.perform(patch("/api/v1/sessions/{id}/favorite", sessionId)
                        .header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(true));
    }

    @Test
    @Order(7)
    void shouldDeleteSession() throws Exception {
        mockMvc.perform(delete("/api/v1/sessions/{id}", sessionId)
                        .header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(8)
    void shouldReturn404AfterDeletion() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/{id}", sessionId)
                        .header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Session not found")));
    }

    @Test
    @Order(9)
    void shouldReturn401WithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/sessions")
                        .queryParam("userId", "integration-user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(10)
    void shouldReturn400WithInvalidRequest() throws Exception {
        CreateSessionRequest request = new CreateSessionRequest();
        // Missing required fields intentionally

        mockMvc.perform(post("/api/v1/sessions")
                        .header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}