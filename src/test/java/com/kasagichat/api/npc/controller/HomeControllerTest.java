package com.kasagichat.api.npc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.kasagichat.api.common.exception.ApiExceptionHandler;
import com.kasagichat.api.npc.controller.dto.response.HomeResponse;
import com.kasagichat.api.npc.controller.dto.response.HomeResponse.ItemResponse;
import com.kasagichat.api.npc.exception.HomeException;
import com.kasagichat.api.npc.model.enums.HomeItemKind;
import com.kasagichat.api.npc.service.HomeService;
import com.kasagichat.api.security.principal.LoginUserPrincipal;

/** standalone MVCテスト。アプリ・DB・実際のSecurityFilterChainは起動しない。 */
@ExtendWith(MockitoExtension.class)
class HomeControllerTest {
    @Mock private HomeService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new HomeController(service))
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
    void readsUsingSessionOwnerAndSerializesHomeContract() throws Exception {
        when(service.getHome(7L)).thenReturn(new HomeResponse(
            new HomeResponse.NpcResponse("分身", "SAMPLE_A", null, 1, 0, null, null),
            List.of(new HomeResponse.SlotResponse("BOOKSHELF_1", HomeItemKind.BOOK)),
            List.of(item()), List.of()));

        mvc.perform(get("/api/home").param("userId", "999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.npc.name").value("分身"))
            .andExpect(jsonPath("$.npc.exp").value(0))
            .andExpect(jsonPath("$.slots[0].acceptedKind").value("BOOK"))
            .andExpect(jsonPath("$.items[0].topicId").value(21))
            .andExpect(jsonPath("$.items[0].kind").value("BOOK"))
            .andExpect(jsonPath("$.items[0].acquiredAt").value("2026-09-20T00:00:00Z"))
            .andExpect(jsonPath("$.unlockedItems").isEmpty());
        verify(service).getHome(7L);
    }

    @Test
    void putsUsingSessionOwnerAndReturnsPlacement() throws Exception {
        when(service.placeItem(7L, 21L, "BOOKSHELF_1")).thenReturn(item());
        mvc.perform(put("/api/home/items/21/placement").contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":\"BOOKSHELF_1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slotId").value("BOOKSHELF_1"));
        verify(service).placeItem(7L, 21L, "BOOKSHELF_1");
    }

    @Test
    void storesUsingSessionOwnerAndReturnsNoContent() throws Exception {
        mvc.perform(delete("/api/home/items/21/placement"))
            .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).storeItem(7L, 21L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"slotId\":null}", "{\"slotId\":\"\"}", "{\"slotId\":\"  \"}",
        "{\"slotId\":\"1234567890123456789012345678901\"}"})
    void validatesSlotBeforeCallingService(String body) throws Exception {
        mvc.perform(put("/api/home/items/21/placement").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(put("/api/home/items/21/placement").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @MethodSource("problems")
    void returnsExistingProblemDetailsFormat(HomeException exception) throws Exception {
        when(service.placeItem(any(), any(), any())).thenThrow(exception);
        mvc.perform(put("/api/home/items/21/placement").contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":\"BOOKSHELF_1\"}"))
            .andExpect(status().is(exception.getStatus().value()))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value(exception.getCode()))
            .andExpect(jsonPath("$.detail").value(exception.getMessage()))
            .andExpect(jsonPath("$.instance").value("/api/home/items/21/placement"));
    }

    @Test
    void storageOfForeignItemReturnsNotFound() throws Exception {
        doThrow(HomeException.itemNotFound()).when(service).storeItem(7L, 999L);
        mvc.perform(delete("/api/home/items/999/placement"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("HOME_ITEM_NOT_FOUND"));
    }

    private static Stream<Arguments> problems() {
        return Stream.of(HomeException.npcNotFound(), HomeException.itemNotFound(), HomeException.invalidSlot(),
                HomeException.incompatibleSlot(), HomeException.occupiedSlot())
            .map(Arguments::of);
    }

    private ItemResponse item() {
        return new ItemResponse(21L, "話題", HomeItemKind.BOOK, null, "話題", null,
            Instant.parse("2026-09-20T00:00:00Z"), null, false, "BOOKSHELF_1");
    }
}
