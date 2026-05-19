package com.kielakjr.job_buddy.tag;

import java.util.UUID;

public class TagNotFoundException extends RuntimeException {
    public TagNotFoundException(UUID id) {
        super("Tag not found: " + id);
    }
}
