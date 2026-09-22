package com.kasagichat.api.event.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.kasagichat.api.common.exception.ApiExceptionHandler;
import com.kasagichat.api.event.controller.dto.response.EventParticipantResponse;
import com.kasagichat.api.event.controller.dto.response.EventResponse;
import com.kasagichat.api.event.exception.EventException;
import com.kasagichat.api.event.model.enums.EventPhase;
import com.kasagichat.api.event.model.enums.VenueTemplate;
import com.kasagichat.api.event.service.EventService;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

/** standalone MVCテスト。アプリ・DB・実際のSecurityFilterChainは起動しない。 */
@ExtendWith(MockitoExtension.class)
class EventControllerTest {
    @Mock private EventService service;
    private MockMvc mvc;
    private final UUID eventId = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new EventController(service))
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
    void createsUsingSessionOwnerAndReturnsCreatedWithInviteCode() throws Exception {
        when(service.create(any(), any())).thenReturn(response(true, "ABCD2345"));

        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"交流会","description":"説明",
                 "startsAt":"2026-03-01T00:00:00Z","endsAt":"2026-03-02T00:00:00Z",
                 "venueTemplate":"HALL"}
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(eventId.toString()))
            .andExpect(jsonPath("$.phase").value("ONGOING"))
            .andExpect(jsonPath("$.inviteCode").value("ABCD2345"));

        verify(service).create(org.mockito.ArgumentMatchers.eq(7L), any());
    }

    @Test
    void rejectsBlankTitleBeforeReachingTheService() throws Exception {
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"  ","startsAt":"2026-03-01T00:00:00Z",
                 "endsAt":"2026-03-02T00:00:00Z","venueTemplate":"HALL"}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(service);
    }

    @Test
    void rejectsUnknownVenueTemplateAsInvalidBody() throws Exception {
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"交流会","startsAt":"2026-03-01T00:00:00Z",
                 "endsAt":"2026-03-02T00:00:00Z","venueTemplate":"ROOFTOP"}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));

        verifyNoInteractions(service);
    }

    @Test
    void hidesInviteCodeFromParticipants() throws Exception {
        when(service.get(7L, eventId)).thenReturn(response(false, null));

        mvc.perform(get("/api/events/{id}", eventId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.owner").value(false))
            .andExpect(jsonPath("$.inviteCode").doesNotExist());
    }

    @Test
    void serializesVenueParticipantsWithoutIdentifiers() throws Exception {
        when(service.participants(7L, eventId))
            .thenReturn(List.of(new EventParticipantResponse("ユウ", "cheerful-girl")));

        mvc.perform(get("/api/events/{id}/participants", eventId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("ユウ"))
            .andExpect(jsonPath("$[0].presetId").value("cheerful-girl"))
            .andExpect(jsonPath("$[0].id").doesNotExist());
    }

    @Test
    void leavesWithNoContent() throws Exception {
        mvc.perform(delete("/api/events/{id}/participants/me", eventId))
            .andExpect(status().isNoContent());

        verify(service).leave(7L, eventId);
    }

    @Test
    void mapsEventWithParticipantsToConflict() throws Exception {
        doThrow(EventException.eventHasParticipants()).when(service).delete(7L, eventId);

        mvc.perform(delete("/api/events/{id}", eventId))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("EVENT_HAS_PARTICIPANTS"));
    }

    @Test
    void mapsMissingEventToNotFound() throws Exception {
        when(service.get(7L, eventId)).thenThrow(EventException.eventNotFound());

        mvc.perform(get("/api/events/{id}", eventId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
    }

    private EventResponse response(boolean owner, String inviteCode) {
        return new EventResponse(
            eventId, "交流会", "説明",
            Instant.parse("2026-03-01T00:00:00Z"), Instant.parse("2026-03-02T00:00:00Z"),
            VenueTemplate.HALL, EventPhase.ONGOING, "分身", 1L, owner, true, inviteCode);
    }
}
