package com.kielakjr.job_buddy.user;

public record OAuthProfile(
    String provider,
    String providerId,
    String email,
    String name,
    String avatarUrl
) {}
