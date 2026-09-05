package com.kasagichat.api.security.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.security.controller.dto.response.TermsResponse;
import com.kasagichat.api.security.service.TermsService;

import lombok.RequiredArgsConstructor;

/**
 * ユーザー登録時に同意が必要な規約を提供するController。
 */
@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermsController {
    
    private final TermsService termsService;

    /**
     * 現在有効な最新規約を規約種類ごとに取得する。
     *
     * @return 同意が必要な最新規約の一覧
     */
    @GetMapping("/required")
    public List<TermsResponse> required() {
        return termsService.getLatestTerms().stream().map(TermsResponse::from).toList();
    }
}
