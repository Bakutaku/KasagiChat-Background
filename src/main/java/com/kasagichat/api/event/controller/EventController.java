package com.kasagichat.api.event.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.event.controller.dto.request.CreateEventRequest;
import com.kasagichat.api.event.controller.dto.response.EventParticipantResponse;
import com.kasagichat.api.event.controller.dto.response.EventResponse;
import com.kasagichat.api.event.service.EventService;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 既存のAPI共通のROLE_USER・セッション認証・CSRF保護を使用する。 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Valid @RequestBody CreateEventRequest request
    ) {
        return eventService.create(principal.userId(), request);
    }

    @GetMapping
    public List<EventResponse> listEvents(@AuthenticationPrincipal LoginUserPrincipal principal) {
        return eventService.list(principal.userId());
    }

    @GetMapping("/{eventId}")
    public EventResponse getEvent(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable UUID eventId
    ) {
        return eventService.get(principal.userId(), eventId);
    }

    @GetMapping("/{eventId}/participants")
    public List<EventParticipantResponse> getParticipants(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable UUID eventId
    ) {
        return eventService.participants(principal.userId(), eventId);
    }

    @DeleteMapping("/{eventId}/participants/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveEvent(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable UUID eventId
    ) {
        eventService.leave(principal.userId(), eventId);
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable UUID eventId
    ) {
        eventService.delete(principal.userId(), eventId);
    }

    @PostMapping("/{eventId}/archive")
    public EventResponse archiveEvent(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable UUID eventId
    ) {
        return eventService.archive(principal.userId(), eventId);
    }
}
