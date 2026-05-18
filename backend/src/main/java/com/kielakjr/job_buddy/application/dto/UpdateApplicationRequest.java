package com.kielakjr.job_buddy.application.dto;

import java.time.LocalDate;

/** Null field = "don't change". Status is intentionally not updatable here. */
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
        String notes) {}
