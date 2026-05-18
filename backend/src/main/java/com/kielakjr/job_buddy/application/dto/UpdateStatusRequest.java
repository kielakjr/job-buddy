package com.kielakjr.job_buddy.application.dto;

import jakarta.validation.constraints.NotNull;

import com.kielakjr.job_buddy.application.ApplicationStatus;

public record UpdateStatusRequest(@NotNull ApplicationStatus status) {}
