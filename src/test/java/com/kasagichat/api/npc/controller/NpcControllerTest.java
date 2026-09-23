package com.kasagichat.api.npc.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.kasagichat.api.common.exception.ApiExceptionHandler;
import com.kasagichat.api.npc.controller.dto.request.UpdateNpcSettingsRequest;
import com.kasagichat.api.npc.controller.dto.response.NpcResponse;
import com.kasagichat.api.npc.exception.NpcNotFoundException;
import com.kasagichat.api.npc.service.NpcService;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

/** standalone MVCテスト。プロフィール帳の部分更新の入力検証だけを対象にする。 */
@ExtendWith(MockitoExtension.class)
class NpcControllerTest {

    @Mock private NpcService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new NpcController(service))
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
    void patchesUsingSessionOwnerAndPassesOmittedFieldsAsNull() throws Exception {
        when(service.updateSettings(any(), any())).thenReturn(response());

        mvc.perform(patch("/api/npc").param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON).content("{\"speechStyleEnabled\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.speechStyleEnabled").value(false))
            .andExpect(jsonPath("$.level").value(3));

        var captor = ArgumentCaptor.forClass(UpdateNpcSettingsRequest.class);
        verify(service).updateSettings(eq(7L), captor.capture());
        assertThat(captor.getValue().profile()).isNull();
        assertThat(captor.getValue().speechStyle()).isNull();
        assertThat(captor.getValue().speechStyleEnabled()).isFalse();
    }

    @Test
    void keepsAnEmptyStringDistinctFromAnOmittedField() throws Exception {
        when(service.updateSettings(any(), any())).thenReturn(response());

        mvc.perform(patch("/api/npc").contentType(MediaType.APPLICATION_JSON)
                .content("{\"profile\":\"\"}"))
            .andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(UpdateNpcSettingsRequest.class);
        verify(service).updateSettings(any(), captor.capture());
        assertThat(captor.getValue().profile()).isEmpty();
        assertThat(captor.getValue().speechStyle()).isNull();
    }

    @Test
    void rejectsAProfileLongerThanTheReviewLimit() throws Exception {
        mvc.perform(patch("/api/npc").contentType(MediaType.APPLICATION_JSON)
                .content("{\"profile\":\"" + "あ".repeat(4001) + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors.profile").exists());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsASpeechStyleLongerThanTheReviewLimit() throws Exception {
        mvc.perform(patch("/api/npc").contentType(MediaType.APPLICATION_JSON)
                .content("{\"speechStyle\":\"" + "あ".repeat(1001) + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors.speechStyle").exists());
        verifyNoInteractions(service);
    }

    @Test
    void acceptsTextsExactlyAtTheLimit() throws Exception {
        when(service.updateSettings(any(), any())).thenReturn(response());

        mvc.perform(patch("/api/npc").contentType(MediaType.APPLICATION_JSON)
                .content("{\"profile\":\"" + "あ".repeat(4000) + "\",\"speechStyle\":\""
                    + "い".repeat(1000) + "\"}"))
            .andExpect(status().isOk());
        verify(service).updateSettings(any(), any());
    }

    @Test
    void returnsProblemDetailsWhenTheNpcIsMissing() throws Exception {
        when(service.updateSettings(any(), any())).thenThrow(new NpcNotFoundException());

        mvc.perform(patch("/api/npc").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("NPC_NOT_FOUND"))
            .andExpect(jsonPath("$.instance").value("/api/npc"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(patch("/api/npc").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
        verifyNoInteractions(service);
    }

    private NpcResponse response() {
        return new NpcResponse("分身", "SAMPLE_A", 3, 120, "人格文書", "口調", false, null);
    }
}
