package com.kasagichat.api.npc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kasagichat.api.master.model.TopicCategory;
import com.kasagichat.api.npc.controller.dto.npc.response.TopicResponse;
import com.kasagichat.api.npc.exception.NpcNotFoundException;
import com.kasagichat.api.npc.exception.TopicNotFoundException;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.model.Topic;
import com.kasagichat.api.npc.repository.TopicRepository;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock
    private TopicRepository topicRepository;
    @Mock
    private NpcService npcService;

    private TopicService service;

    @BeforeEach
    void setUp() {
        service = new TopicService(topicRepository, npcService);
    }

    @Test
    void getTopicsReturnsTopicsWithCategory() {
        Npc npc = Npc.builder().id(5L).build();
        TopicCategory category = TopicCategory.builder()
                .code("GAME")
                .name("ゲーム")
                .displayName("ゲーム機")
                .itemImagePath("/images/mementos/game.png")
                .build();
        Topic topic = topic();
        topic.setCategory(category);
        when(npcService.findOwnNpc(1L)).thenReturn(npc);
        when(topicRepository.findByNpcIdOrderByLearnedAtDesc(5L)).thenReturn(List.of(topic));

        List<TopicResponse> responses = service.getTopics(1L);

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.name()).isEqualTo("ゲーム実況");
            assertThat(response.category().displayName()).isEqualTo("ゲーム機");
            assertThat(response.isPublic()).isFalse();
        });
    }

    @Test
    void getTopicsRejectsMissingNpc() {
        when(npcService.findOwnNpc(1L)).thenThrow(new NpcNotFoundException());

        assertThatThrownBy(() -> service.getTopics(1L)).isInstanceOf(NpcNotFoundException.class);
    }

    @Test
    void updateVisibilitySetsDecidedAt() {
        Topic topic = topic();
        when(topicRepository.findByIdAndNpcUserId(9L, 1L)).thenReturn(Optional.of(topic));

        TopicResponse response = service.updateVisibility(1L, 9L, true);

        assertThat(topic.getPublicTopic()).isTrue();
        assertThat(topic.getVisibilityDecidedAt()).isNotNull();
        assertThat(response.isPublic()).isTrue();
        assertThat(response.category()).isNull();
    }

    @Test
    void updateVisibilityRejectsOthersTopic() {
        when(topicRepository.findByIdAndNpcUserId(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateVisibility(1L, 9L, true))
                .isInstanceOf(TopicNotFoundException.class);
    }

    @Test
    void deleteTopicDeletesOwnTopic() {
        Topic topic = topic();
        when(topicRepository.findByIdAndNpcUserId(9L, 1L)).thenReturn(Optional.of(topic));

        service.deleteTopic(1L, 9L);

        verify(topicRepository).delete(topic);
    }

    @Test
    void deleteTopicRejectsOthersTopic() {
        when(topicRepository.findByIdAndNpcUserId(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteTopic(1L, 9L)).isInstanceOf(TopicNotFoundException.class);

        verify(topicRepository, never()).delete(any());
    }

    private Topic topic() {
        return Topic.builder()
                .id(9L)
                .name("ゲーム実況")
                .interest((short) 3)
                .learnedAt(Instant.now())
                .build();
    }
}
