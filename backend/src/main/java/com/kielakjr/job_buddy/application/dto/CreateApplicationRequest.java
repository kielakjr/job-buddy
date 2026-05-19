package com.kielakjr.job_buddy.application.dto;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApplicationRequest(
        @NotBlank String company,
        @NotBlank String position,
        String location,
        Boolean remote,
        String source,
        String offerUrl,
        Integer salaryMin,
        Integer salaryMax,
        @Size(min = 3, max = 3) String salaryCurrency,
        LocalDate appliedAt,
        String notes,
        Set<UUID> tagIds) {}
