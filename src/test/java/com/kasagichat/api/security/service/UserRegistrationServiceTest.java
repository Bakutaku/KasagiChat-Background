package com.kasagichat.api.security.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kasagichat.api.security.SecurityProperties;
import com.kasagichat.api.security.controller.dto.request.UserRegistrationRequest;
import com.kasagichat.api.security.exception.PendingRegistrationExpiredException;
import com.kasagichat.api.security.exception.PendingRegistrationNotFoundException;
import com.kasagichat.api.security.exception.UserAlreadyRegisteredException;
import com.kasagichat.api.security.model.PendingUsers;
import com.kasagichat.api.security.model.UserAuth;
import com.kasagichat.api.security.model.enums.AuthProvider;
import com.kasagichat.api.security.repository.PendingUsersRepository;
import com.kasagichat.api.security.repository.UserAuthRepository;
import com.kasagichat.api.security.repository.UsersRepository;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock
    private PendingUsersRepository pendingUsersRepository;
    @Mock
    private UserAuthRepository userAuthRepository;
    @Mock
    private UsersRepository usersRepository;
    @Mock
    private TermsService termsService;

    private UserRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new UserRegistrationService(
                new SecurityProperties(URI.create("http://localhost:3000"), Duration.ofMinutes(30)),
                pendingUsersRepository,
                userAuthRepository,
                usersRepository,
                termsService
        );
    }

    @Test
    void registrationRejectsMissingPendingRegistration() {
        when(pendingUsersRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registration(10L, request()))
                .isInstanceOf(PendingRegistrationNotFoundException.class);

        verifyNoInteractions(userAuthRepository, usersRepository, termsService);
    }

    @Test
    void registrationRejectsExpiredPendingRegistration() {
        PendingUsers pendingUser = pendingUser(Instant.now().minusSeconds(1));
        when(pendingUsersRepository.findById(10L)).thenReturn(Optional.of(pendingUser));

        assertThatThrownBy(() -> service.registration(10L, request()))
                .isInstanceOf(PendingRegistrationExpiredException.class);

        verify(userAuthRepository, never()).findByProviderAndSubject(
                pendingUser.getProvider(),
                pendingUser.getSubject()
        );
        verifyNoInteractions(usersRepository, termsService);
    }

    @Test
    void registrationRejectsAlreadyRegisteredOAuthIdentity() {
        PendingUsers pendingUser = pendingUser(Instant.now().plusSeconds(60));
        when(pendingUsersRepository.findById(10L)).thenReturn(Optional.of(pendingUser));
        when(userAuthRepository.findByProviderAndSubject(AuthProvider.GOOGLE, "subject"))
                .thenReturn(Optional.of(new UserAuth()));

        assertThatThrownBy(() -> service.registration(10L, request()))
                .isInstanceOf(UserAlreadyRegisteredException.class);

        verifyNoInteractions(usersRepository, termsService);
    }

    @Test
    void getPendingUsersUsesSameNotFoundException() {
        when(pendingUsersRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPendingUsers(10L))
                .isInstanceOf(PendingRegistrationNotFoundException.class);
    }

    @Test
    void getPendingUsersUsesSameExpiredException() {
        when(pendingUsersRepository.findById(10L))
                .thenReturn(Optional.of(pendingUser(Instant.now().minusSeconds(1))));

        assertThatThrownBy(() -> service.getPendingUsers(10L))
                .isInstanceOf(PendingRegistrationExpiredException.class);
    }

    private UserRegistrationRequest request() {
        return new UserRegistrationRequest("Test User", Set.of(1L));
    }

    private PendingUsers pendingUser(Instant expiresAt) {
        return PendingUsers.builder()
                .id(10L)
                .provider(AuthProvider.GOOGLE)
                .subject("subject")
                .displayName("Test User")
                .expiresAt(expiresAt)
                .build();
    }
}
