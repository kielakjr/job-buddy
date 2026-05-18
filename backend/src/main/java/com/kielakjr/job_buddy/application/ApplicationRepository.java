package com.kielakjr.job_buddy.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Optional<Application> findByIdAndUser_Id(UUID id, UUID userId);

    List<Application> findAllByUser_IdOrderByCreatedAtDesc(UUID userId);

    long deleteByIdAndUser_Id(UUID id, UUID userId);
}
