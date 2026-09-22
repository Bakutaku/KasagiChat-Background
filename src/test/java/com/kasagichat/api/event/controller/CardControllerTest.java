package com.kasagichat.api.event.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.kasagichat.api.common.exception.ApiExceptionHandler;
import com.kasagichat.api.event.controller.dto.response.CardResponse;
import com.kasagichat.api.event.exception.EventException;
import com.kasagichat.api.event.service.CardService;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

/** standalone MVCテスト。アプリ・DB・実際のSecurityFilterChainは起動しない。 */
@ExtendWith(MockitoExtension.class)
class CardControllerTest {
    @Mock private CardService service;
    private MockMvc mvc;
    private final UUID cardId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private final UUID eventId = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new CardController(service))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new ApiExceptionHandler()).build();
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(new LoginUserPrincipal(7L), null, "ROLE_USER"));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void serializesUnopenedCardsWithoutTheReport() throws Exception {
        when(service.listByEvent(7L, eventId)).thenReturn(List.of(unopened()));

        mvc.perform(get("/api/events/{id}/cards", eventId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(cardId.toString()))
            .andExpect(jsonPath("$[0].eventId").value(eventId.toString()))
            .andExpect(jsonPath("$[0].eventTitle").value("交流会"))
            .andExpect(jsonPath("$[0].partnerName").value("ハル"))
            .andExpect(jsonPath("$[0].partnerPresetId").value("cool-girl"))
            .andExpect(jsonPath("$[0].score").value(40))
            .andExpect(jsonPath("$[0].commonTags[0]").value("ゲーム"))
            .andExpect(jsonPath("$[0].opened").value(false))
            .andExpect(jsonPath("$[0].report").doesNotExist())
            .andExpect(jsonPath("$[0].recommendedTopics").doesNotExist());
    }

    @Test
    void listsAllCardsOfTheSessionOwner() throws Exception {
        when(service.list(7L)).thenReturn(List.of(unopened()));

        mvc.perform(get("/api/cards"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].partnerName").value("ハル"));
    }

    @Test
    void returnsTheGeneratedReportOnOpen() throws Exception {
        when(service.open(7L, cardId)).thenReturn(opened());

        mvc.perform(post("/api/cards/{id}/open", cardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.opened").value(true))
            .andExpect(jsonPath("$.report").value("ゲームの話で盛り上がってきたよ。"))
            .andExpect(jsonPath("$.recommendedTopics[0]").value("最近遊んだゲーム"));
    }

    @Test
    void mapsCardsOfOtherUsersToNotFound() throws Exception {
        when(service.get(7L, cardId)).thenThrow(EventException.cardNotFound());

        mvc.perform(get("/api/cards/{id}", cardId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"));
    }

    @Test
    void mapsOpeningBeforeTheEventStartsToConflict() throws Exception {
        when(service.open(7L, cardId)).thenThrow(EventException.eventNotStarted());

        mvc.perform(post("/api/cards/{id}/open", cardId))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("EVENT_NOT_STARTED"));
    }

    private CardResponse unopened() {
        return new CardResponse(
            cardId, eventId, "交流会", "ハル", "cool-girl", 40, List.of("ゲーム"),
            false, null, null, null);
    }

    private CardResponse opened() {
        return new CardResponse(
            cardId, eventId, "交流会", "ハル", "cool-girl", 40, List.of("ゲーム"),
            true, Instant.parse("2026-03-01T10:00:00Z"),
            "ゲームの話で盛り上がってきたよ。", List.of("最近遊んだゲーム"));
    }
}
