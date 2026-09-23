package com.kasagichat.api.npc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.kasagichat.api.common.exception.ApiExceptionHandler;
import com.kasagichat.api.npc.controller.dto.response.HomeResponse.ItemResponse;
import com.kasagichat.api.npc.exception.TopicNotFoundException;
import com.kasagichat.api.npc.model.enums.HomeItemKind;
import com.kasagichat.api.npc.service.TopicService;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

/** standalone MVCテスト。アプリ・DB・実際のSecurityFilterChainは起動しない。 */
@ExtendWith(MockitoExtension.class)
class TopicControllerTest {

    @Mock private TopicService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TopicController(service))
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
    void patchesUsingSessionOwnerAndReturnsTheUpdatedTopic() throws Exception {
        when(service.updateVisibility(7L, 21L, true)).thenReturn(item(true));

        mvc.perform(patch("/api/topics/21").param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON).content("{\"publicTopic\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.topicId").value(21))
            .andExpect(jsonPath("$.publicTopic").value(true))
            .andExpect(jsonPath("$.kind").value("BOOK"))
            .andExpect(jsonPath("$.acquiredAt").value("2026-09-20T00:00:00Z"));
        verify(service).updateVisibility(7L, 21L, true);
    }

    @Test
    void deletesUsingSessionOwnerAndReturnsNoContent() throws Exception {
        mvc.perform(delete("/api/topics/21"))
            .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).delete(7L, 21L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"publicTopic\":null}"})
    void requiresAnExplicitVisibilityBeforeCallingService(String body) throws Exception {
        mvc.perform(patch("/api/topics/21").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(patch("/api/topics/21").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
        verifyNoInteractions(service);
    }

    @Test
    void returnsExistingProblemDetailsFormatForForeignTopics() throws Exception {
        when(service.updateVisibility(any(), any(), any())).thenThrow(new TopicNotFoundException());
        doThrow(new TopicNotFoundException()).when(service).delete(7L, 999L);

        mvc.perform(patch("/api/topics/999").contentType(MediaType.APPLICATION_JSON)
                .content("{\"publicTopic\":false}"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("TOPIC_NOT_FOUND"))
            .andExpect(jsonPath("$.instance").value("/api/topics/999"));
        mvc.perform(delete("/api/topics/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TOPIC_NOT_FOUND"));
    }

    private ItemResponse item(boolean publicTopic) {
        return new ItemResponse(21L, "話題", HomeItemKind.BOOK, null, "話題", null,
            Instant.parse("2026-09-20T00:00:00Z"), null, publicTopic, null);
    }
}
