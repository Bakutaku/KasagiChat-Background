package com.kasagichat.api.security.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kasagichat.api.security.controller.dto.response.TermsResponse;
import com.kasagichat.api.security.service.TermsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermsController {
    
    private final TermsService termsService;

    @GetMapping("/required")
    public List<TermsResponse> required() {
        return termsService.getLatestTerms().stream().map(TermsResponse::from).toList();
    }
}
