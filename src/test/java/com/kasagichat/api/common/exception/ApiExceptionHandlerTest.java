package com.kasagichat.api.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.kasagichat.api.security.exception.PendingRegistrationExpiredException;
import com.kasagichat.api.security.exception.PendingRegistrationNotFoundException;
import com.kasagichat.api.security.exception.TermsAgreementRequiredException;
import com.kasagichat.api.security.exception.UserAlreadyRegisteredException;
import com.kasagichat.api.security.exception.UserNotFoundException;

class ApiExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @ParameterizedTest
    @MethodSource("problems")
    void returnsProblemDetails(
            String type,
            int expectedStatus,
            String expectedTitle,
            String expectedDetail,
            String expectedCode
    ) throws Exception {
        mockMvc.perform(get("/test/problems/{type}", type))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value(expectedTitle))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.detail").value(expectedDetail))
                .andExpect(jsonPath("$.instance").value("/test/problems/" + type))
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    private static Stream<Arguments> problems() {
        return Stream.of(
                Arguments.of(
                        "not-found",
                        401,
                        "Unauthorized",
                        "仮登録情報が存在しません",
                        "PENDING_REGISTRATION_NOT_FOUND"
                ),
                Arguments.of(
                        "expired",
                        401,
                        "Unauthorized",
                        "仮登録の有効期限が切れています",
                        "PENDING_REGISTRATION_EXPIRED"
                ),
                Arguments.of(
                        "registered",
                        409,
                        "Conflict",
                        "すでに登録済みのユーザーです",
                        "USER_ALREADY_REGISTERED"
                ),
                Arguments.of(
                        "user-not-found",
                        404,
                        "Not Found",
                        "ユーザーが見つかりませんでした。",
                        "USER_NOT_FOUND"
                ),
                Arguments.of(
                        "terms",
                        400,
                        "Bad Request",
                        "最新の必須規約への同意が必要です",
                        "TERMS_AGREEMENT_REQUIRED"
                )
        );
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/problems/{type}")
        void throwProblem(@PathVariable String type) {
            throw switch (type) {
                case "not-found" -> new PendingRegistrationNotFoundException();
                case "expired" -> new PendingRegistrationExpiredException();
                case "registered" -> new UserAlreadyRegisteredException();
                case "user-not-found" -> new UserNotFoundException();
                case "terms" -> new TermsAgreementRequiredException();
                default -> new IllegalArgumentException("unknown test type");
            };
        }
    }
}
