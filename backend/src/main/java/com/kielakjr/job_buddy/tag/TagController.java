package com.kielakjr.job_buddy.tag;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kielakjr.job_buddy.auth.CurrentUser;
import com.kielakjr.job_buddy.tag.dto.CreateTagRequest;
import com.kielakjr.job_buddy.tag.dto.TagResponse;
import com.kielakjr.job_buddy.tag.dto.UpdateTagRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService service;
    private final CurrentUser currentUser;

    @PostMapping
    public ResponseEntity<TagResponse> create(
            @AuthenticationPrincipal OAuth2User principal, @Valid @RequestBody CreateTagRequest req) {
        var resp = service.create(currentUser.require(principal), req);
        return ResponseEntity.created(URI.create("/api/tags/" + resp.id())).body(resp);
    }

    @GetMapping
    public List<TagResponse> list(@AuthenticationPrincipal OAuth2User principal) {
        return service.list(currentUser.require(principal));
    }

    @PatchMapping("/{id}")
    public TagResponse update(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTagRequest req) {
        return service.update(currentUser.require(principal), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID id) {
        service.delete(currentUser.require(principal), id);
        return ResponseEntity.noContent().build();
    }
}
