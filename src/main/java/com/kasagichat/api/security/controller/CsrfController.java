package com.kasagichat.api.security.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;


/**
 * 認証関連のAPI
 */
@RestController 
@RequestMapping ("/api/auth")
public class CsrfController {
    
    /**
     * CSRFトークンを取得する
     * @param csrfToken
     * @return {@code CsrfToken}
     */
    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }
}
