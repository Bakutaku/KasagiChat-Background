package com.kasagichat.api.npc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kasagichat.api.npc.controller.dto.growthevent.response.GrowthEventResponse;
import com.kasagichat.api.npc.model.GrowthEvent;
import com.kasagichat.api.npc.model.enums.GrowthEventType;
import com.kasagichat.api.npc.repository.GrowthEventRepository;

@ExtendWith(MockitoExtension.class)
class GrowthEventServiceTest {

    @Mock
    private GrowthEventRepository growthEventRepository;

    private GrowthEventService service;

    @BeforeEach
    void setUp() {
        service = new GrowthEventService(growthEventRepository);
    }

    @Test
    void getGrowthEventsReturnsUnreadOnly() {
        when(growthEventRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(growthEvent()));

        List<GrowthEventResponse> responses = service.getGrowthEvents(1L, true);

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.type()).isEqualTo(GrowthEventType.LEVEL_UP);
            assertThat(response.readAt()).isNull();
        });
        verify(growthEventRepository, never()).findTop50ByUserIdOrderByCreatedAtDesc(any());
    }

    @Test
    void getGrowthEventsReturnsRecentWhenNotUnreadOnly() {
        when(growthEventRepository.findTop50ByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(growthEvent()));

        assertThat(service.getGrowthEvents(1L, false)).hasSize(1);
        verify(growthEventRepository, never()).findByUserIdAndReadAtIsNullOrderByCreatedAtAsc(any());
    }

    @Test
    void markAllAsReadUpdatesUsersUnreadEvents() {
        service.markAllAsRead(1L);

        verify(growthEventRepository).markAllAsRead(eq(1L), any(Instant.class));
    }

    private GrowthEvent growthEvent() {
        return GrowthEvent.builder()
                .id(7L)
                .type(GrowthEventType.LEVEL_UP)
                .message("ユウがレベル2になりました！")
                .build();
    }
}
