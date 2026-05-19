package com.kielakjr.job_buddy.application.dto;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Null field = "don't change". Status is intentionally not updatable here.
 * For tagIds: null = unchanged, [] = clear all, non-empty = replace full set.
 */
public record UpdateApplicationRequest(
        String company,
        String position,
        String location,
        Boolean remote,
        String source,
        String offerUrl,
        Integer salaryMin,
        Integer salaryMax,
        String salaryCurrency,
        LocalDate appliedAt,
        String notes,
        Set<UUID> tagIds) {}
