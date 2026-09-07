package com.kasagichat.api.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kasagichat.api.security.model.Terms;
import com.kasagichat.api.security.model.enums.TermsType;
import com.kasagichat.api.security.repository.TermsRepository;

@ExtendWith(MockitoExtension.class)
class DevelopmentTermsInitializerTest {

    @Mock
    private TermsRepository termsRepository;

    @Test
    void createsTestTermsOnlyWhenNoTermsExist() {
        when(termsRepository.count()).thenReturn(0L);

        new DevelopmentTermsInitializer(termsRepository).run(null);

        ArgumentCaptor<List<Terms>> termsCaptor = ArgumentCaptor.forClass(List.class);
        verify(termsRepository).saveAll(termsCaptor.capture());

        List<Terms> terms = termsCaptor.getValue();
        assertThat(terms)
            .extracting(Terms::getType)
            .containsExactly(TermsType.TERMS_OF_SERVICE, TermsType.PRIVACY_POLICY);
        assertThat(terms)
            .extracting(Terms::getVersion)
            .containsOnly("1.0.0");
        assertThat(terms)
            .allSatisfy(term -> assertThat(term.getEffectiveAt()).isNotNull());
        assertThat(terms.getFirst().getContent())
            .contains("第1条（適用）", "第7条（保証の否認および免責）", "開発中");
    }

    @Test
    void doesNotCreateTestTermsWhenTermsAlreadyExist() {
        when(termsRepository.count()).thenReturn(1L);

        new DevelopmentTermsInitializer(termsRepository).run(null);

        verify(termsRepository, never()).saveAll(any());
    }
}
