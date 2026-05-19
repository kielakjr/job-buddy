package com.kielakjr.job_buddy.tag.dto;

import java.util.UUID;

import com.kielakjr.job_buddy.tag.Tag;

public record TagResponse(UUID id, String name, String color) {

    public static TagResponse from(Tag t) {
        return new TagResponse(t.getId(), t.getName(), t.getColor());
    }
}
