package com.kasagichat.api.common.exception;

import java.net.URI;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

/**
 * アプリケーション固有例外をProblem Details形式のAPIレスポンスへ変換するハンドラー。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * アプリケーション固有例外をProblem Details形式のレスポンスへ変換する。
     *
     * @param exception 発生したアプリケーション固有例外
     * @param request 例外発生時のHTTPリクエスト
     * @return 例外情報を格納したHTTPレスポンス
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ProblemDetail> handleBaseException(
            BaseException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                exception.getStatus(),
                exception.getMessage()
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle(exception.getStatus().getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", exception.getCode());

        return ResponseEntity.status(exception.getStatus()).body(problemDetail);
    }
}
