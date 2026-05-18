package com.kielakjr.job_buddy.application.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.kielakjr.job_buddy.application.Application;
import com.kielakjr.job_buddy.application.ApplicationStatus;

public record ApplicationResponse(
        UUID id,
        String company,
        String position,
        String location,
        Boolean remote,
        ApplicationStatus status,
        String source,
        String offerUrl,
        Integer salaryMin,
        Integer salaryMax,
        String salaryCurrency,
        LocalDate appliedAt,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ApplicationResponse from(Application a) {
        return new ApplicationResponse(
                a.getId(),
                a.getCompany(),
                a.getPosition(),
                a.getLocation(),
                a.getRemote(),
                a.getStatus(),
                a.getSource(),
                a.getOfferUrl(),
                a.getSalaryMin(),
                a.getSalaryMax(),
                a.getSalaryCurrency(),
                a.getAppliedAt(),
                a.getNotes(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }
}
