package com.kasagichat.api.npc.service;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.kasagichat.api.master.model.LevelCurve;
import com.kasagichat.api.master.repository.LevelCurveRepository;
import com.kasagichat.api.npc.model.GrowthEvent;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.model.enums.GrowthEventType;
import com.kasagichat.api.npc.repository.GrowthEventRepository;
import com.kasagichat.api.security.model.Users;

@ExtendWith(MockitoExtension.class)
class LevelServiceTest {

    @Mock
    private LevelCurveRepository levelCurveRepository;
    @Mock
    private GrowthEventRepository growthEventRepository;

    private LevelService service;

    @BeforeEach
    void setUp() {
        service = new LevelService(levelCurveRepository, growthEventRepository);
    }

    @Test
    void applyExpCrossesMultipleLevels() {
        when(levelCurveRepository.findAllByOrderByLevelAsc()).thenReturn(curves());
        Npc npc = npc(1, 10);

        LevelUpResult result = service.applyExp(npc, 45);

        assertThat(result).isEqualTo(new LevelUpResult(3, true));
        assertThat(npc.getExp()).isEqualTo(55);
        assertThat(npc.getLevel()).isEqualTo(3);
        ArgumentCaptor<GrowthEvent> captor = ArgumentCaptor.forClass(GrowthEvent.class);
        verify(growthEventRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(GrowthEventType.LEVEL_UP);
        assertThat(captor.getValue().getMessage()).contains("レベル3");
        assertThat(captor.getValue().getUser()).isSameAs(npc.getUser());
    }

    @Test
    void applyExpWithoutLevelUpDoesNotNotify() {
        when(levelCurveRepository.findAllByOrderByLevelAsc()).thenReturn(curves());
        Npc npc = npc(1, 0);

        LevelUpResult result = service.applyExp(npc, 5);

        assertThat(result).isEqualTo(new LevelUpResult(1, false));
        assertThat(npc.getExp()).isEqualTo(5);
        verify(growthEventRepository, never()).save(any());
    }

    @Test
    void applyExpStaysAtMaxLevel() {
        when(levelCurveRepository.findAllByOrderByLevelAsc()).thenReturn(curves());
        Npc npc = npc(4, 100);

        LevelUpResult result = service.applyExp(npc, 500);

        assertThat(result).isEqualTo(new LevelUpResult(4, false));
        assertThat(npc.getExp()).isEqualTo(600);
    }

    @Test
    void findNextLevelExpReturnsNullAtMaxLevel() {
        when(levelCurveRepository.findById(2)).thenReturn(Optional.of(curve(2, 20)));
        when(levelCurveRepository.findById(5)).thenReturn(Optional.empty());

        assertThat(service.findNextLevelExp(1)).isEqualTo(20);
        assertThat(service.findNextLevelExp(4)).isNull();
    }

    private List<LevelCurve> curves() {
        return List.of(curve(1, 0), curve(2, 20), curve(3, 50), curve(4, 100));
    }

    private LevelCurve curve(int level, int requiredExp) {
        return LevelCurve.builder().level(level).requiredExp(requiredExp).build();
    }

    private Npc npc(int level, int exp) {
        return Npc.builder()
                .user(Users.builder().id(1L).displayName("テストユーザー").build())
                .name("ユウ")
                .presetId("PRESET_01")
                .level(level)
                .exp(exp)
                .build();
    }
}
