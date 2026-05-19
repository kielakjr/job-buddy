package com.kielakjr.job_buddy.application;

import java.util.Set;
import java.util.UUID;

public class InvalidTagIdsException extends RuntimeException {

    private final Set<UUID> unknownIds;

    public InvalidTagIdsException(Set<UUID> unknownIds) {
        super("Unknown tag ids: " + unknownIds);
        this.unknownIds = unknownIds;
    }

    public Set<UUID> getUnknownIds() {
        return unknownIds;
    }
}
