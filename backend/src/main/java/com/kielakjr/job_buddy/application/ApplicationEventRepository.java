package com.kielakjr.job_buddy.application;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationEventRepository extends JpaRepository<ApplicationEvent, UUID> {

    List<ApplicationEvent> findAllByApplication_IdOrderByOccurredAtDesc(UUID applicationId);
}
