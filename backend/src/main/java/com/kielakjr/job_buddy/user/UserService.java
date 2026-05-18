package com.kielakjr.job_buddy.user;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new IllegalStateException("User not found: " + id));
    }

    @Transactional
    public User upsertFromOAuth(OAuthProfile profile) {
        if (profile.email() == null) {
            throw new IllegalArgumentException("OAuth profile missing email");
        }

        var existing =
                switch (profile.provider()) {
                    case "google" ->
                        userRepository.findByGoogleId(profile.providerId()).orElse(null);
                    case "linkedin" ->
                        userRepository.findByLinkedinId(profile.providerId()).orElse(null);
                    default -> null;
                };
        if (existing == null) {
            existing = userRepository
                    .findByEmail(profile.email())
                    .orElseGet(() -> User.builder().email(profile.email()).build());
        }

        existing.setEmail(profile.email());
        if (profile.name() != null) existing.setName(profile.name());
        if (profile.avatarUrl() != null) existing.setAvatarUrl(profile.avatarUrl());
        switch (profile.provider()) {
            case "google" -> existing.setGoogleId(profile.providerId());
            case "linkedin" -> existing.setLinkedinId(profile.providerId());
            default -> {
                /* unknown provider — no id linkage */
            }
        }

        return userRepository.save(existing);
    }
}
