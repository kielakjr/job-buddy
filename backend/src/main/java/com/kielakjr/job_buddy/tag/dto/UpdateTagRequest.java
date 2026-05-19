package com.kielakjr.job_buddy.tag.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Null field = "don't change". Blank-but-present name is rejected by the service. */
public record UpdateTagRequest(@Size(max = 50) String name, @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color) {}
