package com.kielakjr.job_buddy.application;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kielakjr.job_buddy.application.dto.ApplicationResponse;
import com.kielakjr.job_buddy.application.dto.CreateApplicationRequest;
import com.kielakjr.job_buddy.application.dto.UpdateApplicationRequest;
import com.kielakjr.job_buddy.application.dto.UpdateStatusRequest;
import com.kielakjr.job_buddy.auth.CurrentUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService service;
    private final CurrentUser currentUser;

    @PostMapping
    public ResponseEntity<ApplicationResponse> create(
            @AuthenticationPrincipal OAuth2User principal, @Valid @RequestBody CreateApplicationRequest req) {
        var resp = service.create(currentUser.require(principal), req);
        return ResponseEntity.created(URI.create("/api/applications/" + resp.id()))
                .body(resp);
    }

    @GetMapping
    public List<ApplicationResponse> list(@AuthenticationPrincipal OAuth2User principal) {
        return service.list(currentUser.require(principal));
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID id) {
        return service.get(currentUser.require(principal), id);
    }

    @PatchMapping("/{id}")
    public ApplicationResponse update(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID id,
            @RequestBody UpdateApplicationRequest req) {
        return service.update(currentUser.require(principal), id, req);
    }

    @PutMapping("/{id}/status")
    public ApplicationResponse changeStatus(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest req) {
        return service.changeStatus(currentUser.require(principal), id, req.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID id) {
        service.delete(currentUser.require(principal), id);
        return ResponseEntity.noContent().build();
    }
}
