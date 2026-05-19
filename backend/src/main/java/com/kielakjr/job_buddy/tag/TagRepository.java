package com.kielakjr.job_buddy.tag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    Optional<Tag> findByIdAndUser_Id(UUID id, UUID userId);

    List<Tag> findAllByUser_IdOrderByNameAsc(UUID userId);

    List<Tag> findAllByIdInAndUser_Id(List<UUID> ids, UUID userId);

    long deleteByIdAndUser_Id(UUID id, UUID userId);
}
