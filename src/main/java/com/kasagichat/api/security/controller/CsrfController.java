package com.kasagichat.api.security.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * CSRF Cookieを初期化するController。
 */
@RestController
@RequestMapping("/api/auth")
public class CsrfController {
    
    /**
     * CSRFトークンを生成し、Cookieへ保存する。
     *
     * <p>クライアントはレスポンスの {@code XSRF-TOKEN} Cookieの値を
     * {@code X-XSRF-TOKEN} ヘッダーへ設定して、状態を変更するリクエストを送信する。</p>
     *
     * @param csrfToken Cookie生成をトリガーするSpring SecurityのCSRFトークン
     * @return コンテンツなしのレスポンス
     */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        return ResponseEntity.noContent().build();
    }
}
