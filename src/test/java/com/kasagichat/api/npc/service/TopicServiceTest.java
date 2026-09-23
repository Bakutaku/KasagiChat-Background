package com.kasagichat.api.npc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kasagichat.api.master.model.TopicCategory;
import com.kasagichat.api.npc.exception.TopicNotFoundException;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.model.Topic;
import com.kasagichat.api.npc.model.enums.HomeItemKind;
import com.kasagichat.api.npc.repository.TopicRepository;
import com.kasagichat.api.security.exception.UserNotFoundException;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.service.UserService;

/** DBやSpringコンテキストを起動しない、所有権・公開判断・削除の単体テスト。 */
@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock private UserService userService;
    @Mock private TopicRepository topicRepository;

    private TopicService service;
    private Topic topic;
    private final Instant learnedAt = Instant.parse("2026-09-20T00:00:00Z");

    @BeforeEach
    void setUp() {
        service = new TopicService(userService, topicRepository);
        Users user = Users.builder().id(7L).displayName("本人").build();
        Npc npc = Npc.builder().id(11L).user(user).name("分身").presetId("SAMPLE_A").build();
        topic = Topic.builder().id(21L).npc(npc).name("最近読んだ本").interest((short) 3)
            .learnedAt(learnedAt).publicTopic(false)
            .category(TopicCategory.builder().id(1L).code("READING").name("読書")
                .displayName("思い出の本").itemImagePath("/images/reading.png").build())
            .build();
    }

    @Test
    void recordsTheFirstVisibilityDecisionAndReturnsTheUpdatedTopic() {
        stubOwnedTopic();
        Instant before = Instant.now();

        var result = service.updateVisibility(7L, 21L, true);

        assertThat(topic.getPublicTopic()).isTrue();
        assertThat(topic.getVisibilityDecidedAt()).isBetween(before, Instant.now());
        assertThat(result.topicId()).isEqualTo(21L);
        assertThat(result.publicTopic()).isTrue();
        assertThat(result.kind()).isEqualTo(HomeItemKind.SOUVENIR);
        assertThat(result.acquiredAt()).isEqualTo(learnedAt);
        verify(topicRepository).saveAndFlush(topic);
    }

    @Test
    void keepsTheOriginalDecisionTimeWhenTogglingAgain() {
        stubOwnedTopic();
        Instant decidedAt = Instant.parse("2026-09-21T00:00:00Z");
        topic.setPublicTopic(true);
        topic.setVisibilityDecidedAt(decidedAt);

        assertThat(service.updateVisibility(7L, 21L, false).publicTopic()).isFalse();

        assertThat(topic.getPublicTopic()).isFalse();
        assertThat(topic.getVisibilityDecidedAt()).isEqualTo(decidedAt);
    }

    @Test
    void doesNotWriteWhenTheDecidedValueIsResent() {
        stubOwnedTopic();
        topic.setPublicTopic(true);
        topic.setVisibilityDecidedAt(Instant.parse("2026-09-21T00:00:00Z"));

        assertThat(service.updateVisibility(7L, 21L, true).publicTopic()).isTrue();

        verify(topicRepository, never()).saveAndFlush(any());
    }

    @Test
    void recordsTheDecisionEvenWhenTheValueDoesNotChangeOnTheFirstTime() {
        stubOwnedTopic();

        service.updateVisibility(7L, 21L, false);

        assertThat(topic.getPublicTopic()).isFalse();
        assertThat(topic.getVisibilityDecidedAt()).isNotNull();
        verify(topicRepository).saveAndFlush(topic);
    }

    @Test
    void deletesThePhysicalRowSoThatMemoriesAndPlacementGoWithIt() {
        stubOwnedTopic();
        topic.setHomeSlotId("DISPLAY_1");

        service.delete(7L, 21L);

        var order = inOrder(topicRepository);
        order.verify(topicRepository).findByIdAndNpcUserId(21L, 7L);
        order.verify(topicRepository).delete(topic);
        order.verify(topicRepository).flush();
        verify(topicRepository, never()).saveAndFlush(any());
    }

    @Test
    void missingOrForeignTopicCannotBeUpdatedOrDeleted() {
        when(topicRepository.findByIdAndNpcUserId(999L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateVisibility(7L, 999L, true))
            .isInstanceOfSatisfying(TopicNotFoundException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("TOPIC_NOT_FOUND"));
        assertThatThrownBy(() -> service.delete(7L, 999L))
            .isInstanceOf(TopicNotFoundException.class);

        verify(topicRepository, never()).saveAndFlush(any());
        verify(topicRepository, never()).delete(any());
    }

    @Test
    void rejectsDeletedUserBeforeTouchingTopics() {
        when(userService.getCurrentUser(7L)).thenThrow(new UserNotFoundException());

        assertThatThrownBy(() -> service.delete(7L, 21L)).isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(topicRepository);
    }

    private void stubOwnedTopic() {
        when(topicRepository.findByIdAndNpcUserId(21L, 7L)).thenReturn(Optional.of(topic));
    }
}
