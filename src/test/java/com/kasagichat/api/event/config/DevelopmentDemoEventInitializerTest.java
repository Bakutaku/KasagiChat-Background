package com.kasagichat.api.event.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kasagichat.api.event.model.Event;
import com.kasagichat.api.event.model.EventParticipant;
import com.kasagichat.api.event.repository.EventParticipantRepository;
import com.kasagichat.api.event.repository.EventRepository;
import com.kasagichat.api.master.repository.TopicCategoryRepository;
import com.kasagichat.api.npc.model.Memory;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.model.Topic;
import com.kasagichat.api.npc.repository.MemoryRepository;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.npc.repository.TopicRepository;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.repository.UsersRepository;

@ExtendWith(MockitoExtension.class)
class DevelopmentDemoEventInitializerTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventParticipantRepository eventParticipantRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private NpcRepository npcRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private MemoryRepository memoryRepository;

    @Mock
    private TopicCategoryRepository topicCategoryRepository;

    @Test
    void createsDemoEventWithSampleNpcsWhenNotExists() {
        when(eventRepository.findByInviteCode(DevelopmentDemoEventInitializer.DEMO_INVITE_CODE))
            .thenReturn(Optional.empty());

        initializer().run(null);

        ArgumentCaptor<Event> event = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getInviteCode()).isEqualTo(DevelopmentDemoEventInitializer.DEMO_INVITE_CODE);
        assertThat(event.getValue().getEndsAt()).isAfter(Instant.parse("2090-01-01T00:00:00Z"));

        ArgumentCaptor<List<Users>> users = ArgumentCaptor.forClass(List.class);
        verify(usersRepository).saveAll(users.capture());
        assertThat(users.getValue()).hasSize(3);

        ArgumentCaptor<List<Npc>> npcs = ArgumentCaptor.forClass(List.class);
        verify(npcRepository).saveAll(npcs.capture());
        assertThat(npcs.getValue()).allSatisfy(npc -> {
            assertThat(npc.getBornAt()).isNotNull();
            assertThat(npc.getAppearance()).isNotEmpty();
        });

        ArgumentCaptor<List<Topic>> topics = ArgumentCaptor.forClass(List.class);
        verify(topicRepository).saveAll(topics.capture());
        assertThat(topics.getValue()).allSatisfy(topic -> assertThat(topic.getVisibilityDecidedAt()).isNotNull());
        assertThat(topics.getValue()).anySatisfy(topic -> assertThat(topic.getPublicTopic()).isFalse());

        ArgumentCaptor<List<Memory>> memories = ArgumentCaptor.forClass(List.class);
        verify(memoryRepository).saveAll(memories.capture());
        assertThat(memories.getValue()).hasSameSizeAs(topics.getValue());

        ArgumentCaptor<List<EventParticipant>> participants = ArgumentCaptor.forClass(List.class);
        verify(eventParticipantRepository).saveAll(participants.capture());
        assertThat(participants.getValue()).hasSize(3);
    }

    @Test
    void doesNotCreateDemoEventWhenAlreadyExists() {
        when(eventRepository.findByInviteCode(DevelopmentDemoEventInitializer.DEMO_INVITE_CODE))
            .thenReturn(Optional.of(new Event()));

        initializer().run(null);

        verify(usersRepository, never()).saveAll(any());
        verify(eventRepository, never()).save(any());
        verify(eventParticipantRepository, never()).saveAll(any());
    }

    private DevelopmentDemoEventInitializer initializer() {
        return new DevelopmentDemoEventInitializer(
            eventRepository,
            eventParticipantRepository,
            usersRepository,
            npcRepository,
            topicRepository,
            memoryRepository,
            topicCategoryRepository
        );
    }
}
