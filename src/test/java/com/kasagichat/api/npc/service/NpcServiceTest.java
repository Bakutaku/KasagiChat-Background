package com.kasagichat.api.npc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.kasagichat.api.master.model.CounterDef;
import com.kasagichat.api.npc.controller.dto.npc.request.CreateNpcRequest;
import com.kasagichat.api.npc.controller.dto.npc.request.UpdateNpcRequest;
import com.kasagichat.api.npc.controller.dto.npc.response.NpcResponse;
import com.kasagichat.api.npc.exception.NpcAlreadyExistsException;
import com.kasagichat.api.npc.exception.NpcNotFoundException;
import com.kasagichat.api.npc.model.AchievementCounter;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.model.enums.NpcPreset;
import com.kasagichat.api.npc.repository.AchievementCounterRepository;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.npc.repository.TopicRepository;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.service.UserService;

@ExtendWith(MockitoExtension.class)
class NpcServiceTest {

    @Mock
    private NpcRepository npcRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private AchievementCounterRepository achievementCounterRepository;
    @Mock
    private UserService userService;
    @Mock
    private LevelService levelService;

    private NpcService service;

    @BeforeEach
    void setUp() {
        service = new NpcService(npcRepository, topicRepository, achievementCounterRepository, userService, levelService);
    }

    @Test
    void createNpcCreatesUnbornNpc() {
        Users user = user();
        when(npcRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userService.getCurrentUser(1L)).thenReturn(user);
        when(npcRepository.saveAndFlush(any(Npc.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(levelService.findNextLevelExp(1)).thenReturn(20);

        NpcResponse response = service.createNpc(1L, new CreateNpcRequest(NpcPreset.PRESET_02, "  ユウ  "));

        ArgumentCaptor<Npc> captor = ArgumentCaptor.forClass(Npc.class);
        verify(npcRepository).saveAndFlush(captor.capture());
        Npc saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getName()).isEqualTo("ユウ");
        assertThat(saved.getPresetId()).isEqualTo("PRESET_02");
        assertThat(saved.getBornAt()).isNull();

        assertThat(response.level()).isEqualTo(1);
        assertThat(response.exp()).isZero();
        assertThat(response.nextLevelExp()).isEqualTo(20);
        assertThat(response.expToNextLevel()).isEqualTo(20);
        assertThat(response.bornAt()).isNull();
    }

    @Test
    void createNpcRejectsWhenNpcAlreadyExists() {
        when(npcRepository.findByUserId(1L)).thenReturn(Optional.of(npc(user())));

        assertThatThrownBy(() -> service.createNpc(1L, new CreateNpcRequest(NpcPreset.PRESET_01, "ユウ")))
                .isInstanceOf(NpcAlreadyExistsException.class);

        verify(npcRepository, never()).saveAndFlush(any());
    }

    @Test
    void createNpcConvertsUniqueConstraintViolation() {
        when(npcRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userService.getCurrentUser(1L)).thenReturn(user());
        when(npcRepository.saveAndFlush(any(Npc.class)))
                .thenThrow(new DataIntegrityViolationException("uk_npcs_user_id"));

        assertThatThrownBy(() -> service.createNpc(1L, new CreateNpcRequest(NpcPreset.PRESET_01, "ユウ")))
                .isInstanceOf(NpcAlreadyExistsException.class);
    }

    @Test
    void getNpcReturnsStatsAndNullNextLevelAtMaxLevel() {
        Npc npc = npc(user());
        npc.setLevel(10);
        npc.setExp(900);
        AchievementCounter counter = AchievementCounter.builder()
                .counterDef(CounterDef.builder().code("PRACTICE_CAFE").name("カフェでの練習回数").build())
                .value(3L)
                .build();
        when(npcRepository.findByUserId(1L)).thenReturn(Optional.of(npc));
        when(levelService.findNextLevelExp(10)).thenReturn(null);
        when(topicRepository.countByNpcId(5L)).thenReturn(4L);
        when(achievementCounterRepository.findByUserId(1L)).thenReturn(List.of(counter));

        NpcResponse response = service.getNpc(1L);

        assertThat(response.nextLevelExp()).isNull();
        assertThat(response.expToNextLevel()).isNull();
        assertThat(response.stats().topicCount()).isEqualTo(4L);
        assertThat(response.stats().counters())
                .singleElement()
                .satisfies(found -> {
                    assertThat(found.code()).isEqualTo("PRACTICE_CAFE");
                    assertThat(found.value()).isEqualTo(3L);
                });
    }

    @Test
    void getNpcRejectsMissingNpc() {
        when(npcRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNpc(1L)).isInstanceOf(NpcNotFoundException.class);
    }

    @Test
    void updateNpcChangesOnlyGivenFields() {
        Npc npc = npc(user());
        npc.setProfile("元の人格");
        npc.setSpeechStyle("元の口調");
        when(npcRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(npc));

        NpcResponse response = service.updateNpc(1L, new UpdateNpcRequest(null, "新しい口調", false));

        assertThat(npc.getProfile()).isEqualTo("元の人格");
        assertThat(npc.getSpeechStyle()).isEqualTo("新しい口調");
        assertThat(npc.getSpeechStyleEnabled()).isFalse();
        assertThat(response.speechStyle()).isEqualTo("新しい口調");
    }

    @Test
    void updateNpcRejectsMissingNpcUsingWriteLockQuery() {
        when(npcRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateNpc(1L, new UpdateNpcRequest("人格", null, null)))
                .isInstanceOf(NpcNotFoundException.class);
    }

    private Users user() {
        return Users.builder().id(1L).displayName("テストユーザー").build();
    }

    private Npc npc(Users user) {
        return Npc.builder().id(5L).user(user).name("ユウ").presetId("PRESET_01").build();
    }
}
