package com.kasagichat.api.common.exception;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    /**
     * Bean Validation違反を入力値を含めないProblem Detailsへ変換する。
     *
     * @param exception バリデーション例外
     * @param request 例外発生時のHTTPリクエスト
     * @return フィールドごとのエラーを格納したレスポンス
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
            errors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        ProblemDetail problemDetail = validationProblem(
            request,
            "リクエストの入力値が不正です",
            "VALIDATION_FAILED"
        );
        problemDetail.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problemDetail);
    }

    /**
     * 不正なJSONや未知のenum値を統一エラーへ変換する。
     *
     * @param exception JSON変換例外
     * @param request 例外発生時のHTTPリクエスト
     * @return 入力エラーレスポンス
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(validationProblem(
            request,
            "JSONの形式または値が不正です",
            "INVALID_REQUEST_BODY"
        ));
    }

    private ProblemDetail validationProblem(HttpServletRequest request, String detail, String code) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            org.springframework.http.HttpStatus.BAD_REQUEST,
            detail
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Bad Request");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", code);
        return problemDetail;
    }
}
