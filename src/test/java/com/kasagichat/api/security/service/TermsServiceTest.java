package com.kasagichat.api.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kasagichat.api.security.exception.TermsAgreementRequiredException;
import com.kasagichat.api.security.model.Terms;
import com.kasagichat.api.security.repository.TermsRepository;
import com.kasagichat.api.security.repository.UserTermsAgreementRepository;

@ExtendWith(MockitoExtension.class)
class TermsServiceTest {

    @Mock
    private TermsRepository termsRepository;
    @Mock
    private UserTermsAgreementRepository userTermsAgreementRepository;

    private TermsService service;

    @BeforeEach
    void setUp() {
        service = new TermsService(termsRepository, userTermsAgreementRepository);
    }

    @Test
    void rejectsWhenNoEffectiveTermsExist() {
        when(termsRepository.findLatestByTypeEffectiveAtLessThanEqual(any()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.validateAndGetLatestTerms(Set.of()))
                .isInstanceOf(TermsAgreementRequiredException.class);
    }

    @Test
    void rejectsNullAgreementIds() {
        Terms terms = terms(1L);
        when(termsRepository.findLatestByTypeEffectiveAtLessThanEqual(any()))
                .thenReturn(List.of(terms));

        assertThatThrownBy(() -> service.validateAndGetLatestTerms(null))
                .isInstanceOf(TermsAgreementRequiredException.class);
    }

    @Test
    void rejectsWhenARequiredAgreementIdIsMissing() {
        when(termsRepository.findLatestByTypeEffectiveAtLessThanEqual(any()))
                .thenReturn(List.of(terms(1L), terms(2L)));

        assertThatThrownBy(() -> service.validateAndGetLatestTerms(Set.of(1L)))
                .isInstanceOf(TermsAgreementRequiredException.class);
    }

    @Test
    void returnsLatestTermsWhenAllRequiredIdsAreIncluded() {
        List<Terms> latestTerms = List.of(terms(1L), terms(2L));
        when(termsRepository.findLatestByTypeEffectiveAtLessThanEqual(any()))
                .thenReturn(latestTerms);

        List<Terms> result = service.validateAndGetLatestTerms(Set.of(1L, 2L, 999L));

        assertThat(result).isEqualTo(latestTerms);
    }

    private Terms terms(Long id) {
        Terms terms = new Terms();
        terms.setId(id);
        return terms;
    }
}
