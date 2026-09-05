package com.kasagichat.api.security.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;


/**
 * 認証に必要なCSRFトークンを提供するController。
 */
@RestController 
@RequestMapping ("/api/auth")
public class CsrfController {
    
    /**
     * 現在のリクエストに対応するCSRFトークンを取得する。
     *
     * @param csrfToken Spring Securityが生成したCSRFトークン
     * @return 現在のCSRFトークン
     */
    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }
}
