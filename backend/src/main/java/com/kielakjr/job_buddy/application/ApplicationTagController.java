package com.kielakjr.job_buddy.application;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kielakjr.job_buddy.auth.CurrentUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/applications/{appId}/tags")
@RequiredArgsConstructor
public class ApplicationTagController {

    private final ApplicationService service;
    private final CurrentUser currentUser;

    @PostMapping("/{tagId}")
    public ResponseEntity<Void> attach(
            @AuthenticationPrincipal OAuth2User principal, @PathVariable UUID appId, @PathVariable UUID tagId) {
        service.attachTag(currentUser.require(principal), appId, tagId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> detach(
            @AuthenticationPrincipal OAuth2User principal, @PathVariable UUID appId, @PathVariable UUID tagId) {
        service.detachTag(currentUser.require(principal), appId, tagId);
        return ResponseEntity.noContent().build();
    }
}
