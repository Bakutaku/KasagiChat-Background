package com.kasagichat.api.event.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import com.kasagichat.api.event.controller.dto.response.EventResponse;
import com.kasagichat.api.event.controller.dto.response.InvitationResponse;
import com.kasagichat.api.event.exception.EventException;
import com.kasagichat.api.event.model.enums.EventPhase;
import com.kasagichat.api.event.model.enums.VenueTemplate;
import com.kasagichat.api.event.service.EventService;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

/** standalone MVCテスト。アプリ・DB・実際のSecurityFilterChainは起動しない。 */
@ExtendWith(MockitoExtension.class)
class InvitationControllerTest {
    @Mock private EventService service;
    private MockMvc mvc;
    private final UUID eventId = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private final Instant startsAt = Instant.parse("2026-03-01T00:00:00Z");
    private final Instant endsAt = Instant.parse("2026-03-02T00:00:00Z");

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new InvitationController(service))
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
    void returnsSummaryWithoutInviteCodeOrParticipants() throws Exception {
        when(service.invitation(7L, "DEMO2026")).thenReturn(new InvitationResponse(
            eventId, "交流会", "説明", startsAt, endsAt,
            VenueTemplate.HALL, EventPhase.ONGOING, "分身", 3L, false));

        mvc.perform(get("/api/invitations/{code}", "DEMO2026"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventId").value(eventId.toString()))
            .andExpect(jsonPath("$.participantCount").value(3))
            .andExpect(jsonPath("$.joined").value(false))
            .andExpect(jsonPath("$.inviteCode").doesNotExist());
    }

    @Test
    void passesTheCodeThroughUntouchedSoTheServiceCanNormalizeIt() throws Exception {
        when(service.join(7L, "demo2026")).thenReturn(new EventResponse(
            eventId, "交流会", "説明", startsAt, endsAt,
            VenueTemplate.HALL, EventPhase.ONGOING, "分身", 4L, false, true, null));

        mvc.perform(post("/api/invitations/{code}/join", "demo2026"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.joined").value(true));

        verify(service).join(7L, "demo2026");
    }

    @Test
    void mapsEndedEventToConflict() throws Exception {
        when(service.join(7L, "DEMO2026")).thenThrow(EventException.eventEnded());

        mvc.perform(post("/api/invitations/{code}/join", "DEMO2026"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("EVENT_ENDED"));
    }

    @Test
    void mapsUnbornNpcToConflict() throws Exception {
        when(service.join(7L, "DEMO2026")).thenThrow(EventException.npcNotBorn());

        mvc.perform(post("/api/invitations/{code}/join", "DEMO2026"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("NPC_NOT_BORN"));
    }
}
