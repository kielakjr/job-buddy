package com.kielakjr.job_buddy.application;

import java.util.UUID;

public class ApplicationNotFoundException extends RuntimeException {
    public ApplicationNotFoundException(UUID id) {
        super("Application not found: " + id);
    }
}
