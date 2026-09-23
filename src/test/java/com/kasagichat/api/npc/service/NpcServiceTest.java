package com.kasagichat.api.npc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kasagichat.api.credential.repository.ApiCredentialRepository;
import com.kasagichat.api.npc.controller.dto.request.UpdateNpcSettingsRequest;
import com.kasagichat.api.npc.exception.NpcNotFoundException;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.service.UserService;

/** プロフィール帳からの部分更新で、nullが「変更しない」として扱われることを確かめる。 */
@ExtendWith(MockitoExtension.class)
class NpcServiceTest {

    @Mock private NpcRepository npcRepository;
    @Mock private ApiCredentialRepository apiCredentialRepository;
    @Mock private UserService userService;

    private NpcService service;
    private Npc npc;

    @BeforeEach
    void setUp() {
        service = new NpcService(npcRepository, apiCredentialRepository, userService);
        npc = Npc.builder().id(11L).user(Users.builder().id(7L).build())
            .name("分身").presetId("SAMPLE_A").level(3).exp(120)
            .profile("もとの人格文書").speechStyle("もとの口調").speechStyleEnabled(true).build();
    }

    @Test
    void updatesOnlyTheFieldsThatWereSent() {
        stubLockedNpc();

        var response = service.updateSettings(7L, new UpdateNpcSettingsRequest("書き直した人格", null, null));

        assertThat(npc.getProfile()).isEqualTo("書き直した人格");
        assertThat(npc.getSpeechStyle()).isEqualTo("もとの口調");
        assertThat(npc.getSpeechStyleEnabled()).isTrue();
        assertThat(response.profile()).isEqualTo("書き直した人格");
        verify(npcRepository).findByUserIdForUpdate(7L);
        verify(npcRepository).saveAndFlush(npc);
    }

    @Test
    void togglesSpeechStyleWithoutTouchingTheTexts() {
        stubLockedNpc();

        var response = service.updateSettings(7L, new UpdateNpcSettingsRequest(null, null, false));

        assertThat(npc.getSpeechStyleEnabled()).isFalse();
        assertThat(npc.getProfile()).isEqualTo("もとの人格文書");
        assertThat(npc.getSpeechStyle()).isEqualTo("もとの口調");
        assertThat(response.speechStyleEnabled()).isFalse();
    }

    @Test
    void treatsAnEmptyStringAsClearingTheText() {
        stubLockedNpc();

        var response = service.updateSettings(7L, new UpdateNpcSettingsRequest("", "", null));

        assertThat(npc.getProfile()).isEmpty();
        assertThat(npc.getSpeechStyle()).isEmpty();
        assertThat(response.profile()).isEmpty();
        assertThat(response.speechStyle()).isEmpty();
    }

    @Test
    void keepsEverythingWhenNothingWasSent() {
        stubLockedNpc();

        var response = service.updateSettings(7L, new UpdateNpcSettingsRequest(null, null, null));

        assertThat(response.profile()).isEqualTo("もとの人格文書");
        assertThat(response.speechStyle()).isEqualTo("もとの口調");
        assertThat(response.speechStyleEnabled()).isTrue();
    }

    @Test
    void neverChangesTheMachineCalculatedStatistics() {
        stubLockedNpc();

        var response = service.updateSettings(7L, new UpdateNpcSettingsRequest("別の人格", "別の口調", false));

        assertThat(response.level()).isEqualTo(3);
        assertThat(response.exp()).isEqualTo(120);
        assertThat(response.name()).isEqualTo("分身");
        assertThat(response.presetId()).isEqualTo("SAMPLE_A");
    }

    @Test
    void rejectsUpdateWhenTheNpcDoesNotExist() {
        when(npcRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSettings(7L, new UpdateNpcSettingsRequest("人格", null, null)))
            .isInstanceOfSatisfying(NpcNotFoundException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("NPC_NOT_FOUND"));

        verify(npcRepository, never()).saveAndFlush(any());
    }

    private void stubLockedNpc() {
        when(npcRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(npc));
    }
}
