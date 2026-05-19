package com.kielakjr.job_buddy.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kielakjr.job_buddy.application.dto.ApplicationResponse;
import com.kielakjr.job_buddy.application.dto.CreateApplicationRequest;
import com.kielakjr.job_buddy.application.dto.UpdateApplicationRequest;
import com.kielakjr.job_buddy.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationEventRepository eventRepository;
    private final com.kielakjr.job_buddy.tag.TagRepository tagRepository;

    @Transactional
    public ApplicationResponse create(User user, CreateApplicationRequest req) {
        var app = Application.builder()
                .user(user)
                .company(req.company())
                .position(req.position())
                .location(req.location())
                .remote(req.remote())
                .status(ApplicationStatus.DRAFT)
                .source(req.source())
                .offerUrl(req.offerUrl())
                .salaryMin(req.salaryMin())
                .salaryMax(req.salaryMax())
                .salaryCurrency(req.salaryCurrency())
                .appliedAt(req.appliedAt())
                .notes(req.notes())
                .tags(resolveOwnedTags(user, req.tagIds()))
                .build();
        return ApplicationResponse.from(applicationRepository.save(app));
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> list(User user) {
        return applicationRepository.findAllByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse get(User user, UUID id) {
        return ApplicationResponse.from(loadOwned(user, id));
    }

    @Transactional
    public ApplicationResponse update(User user, UUID id, UpdateApplicationRequest req) {
        var app = loadOwned(user, id);
        if (req.company() != null) app.setCompany(req.company());
        if (req.position() != null) app.setPosition(req.position());
        if (req.location() != null) app.setLocation(req.location());
        if (req.remote() != null) app.setRemote(req.remote());
        if (req.source() != null) app.setSource(req.source());
        if (req.offerUrl() != null) app.setOfferUrl(req.offerUrl());
        if (req.salaryMin() != null) app.setSalaryMin(req.salaryMin());
        if (req.salaryMax() != null) app.setSalaryMax(req.salaryMax());
        if (req.salaryCurrency() != null) app.setSalaryCurrency(req.salaryCurrency());
        if (req.appliedAt() != null) app.setAppliedAt(req.appliedAt());
        if (req.notes() != null) app.setNotes(req.notes());
        if (req.tagIds() != null) {
            if (req.tagIds().isEmpty()) {
                app.getTags().clear();
            } else {
                var resolved = resolveOwnedTags(user, req.tagIds());
                app.getTags().clear();
                app.getTags().addAll(resolved);
            }
        }
        return ApplicationResponse.from(applicationRepository.save(app));
    }

    @Transactional
    public ApplicationResponse changeStatus(User user, UUID id, ApplicationStatus newStatus) {
        var app = loadOwned(user, id);
        var previous = app.getStatus();
        if (previous == newStatus) {
            return ApplicationResponse.from(app);
        }
        app.setStatus(newStatus);
        var saved = applicationRepository.save(app);
        eventRepository.save(ApplicationEvent.builder()
                .application(app)
                .eventType("STATUS_CHANGED")
                .metadata(Map.of("from", previous.name(), "to", newStatus.name()))
                .build());
        return ApplicationResponse.from(saved);
    }

    @Transactional
    public void delete(User user, UUID id) {
        long removed = applicationRepository.deleteByIdAndUser_Id(id, user.getId());
        if (removed == 0) {
            throw new ApplicationNotFoundException(id);
        }
    }

    @Transactional
    public void attachTag(User user, UUID appId, UUID tagId) {
        var app = loadOwned(user, appId);
        var tag = tagRepository
                .findByIdAndUser_Id(tagId, user.getId())
                .orElseThrow(() -> new com.kielakjr.job_buddy.tag.TagNotFoundException(tagId));
        app.getTags().add(tag);
        applicationRepository.save(app);
    }

    @Transactional
    public void detachTag(User user, UUID appId, UUID tagId) {
        var app = loadOwned(user, appId);
        app.getTags().removeIf(t -> t.getId().equals(tagId));
        applicationRepository.save(app);
    }

    private Application loadOwned(User user, UUID id) {
        return applicationRepository
                .findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    private java.util.Set<com.kielakjr.job_buddy.tag.Tag> resolveOwnedTags(
            User user, java.util.Set<java.util.UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new java.util.HashSet<>();
        }
        var found = tagRepository.findAllByIdInAndUser_Id(java.util.List.copyOf(ids), user.getId());
        if (found.size() != ids.size()) {
            var foundIds = found.stream()
                    .map(com.kielakjr.job_buddy.tag.Tag::getId)
                    .collect(java.util.stream.Collectors.toSet());
            var missing = new java.util.HashSet<>(ids);
            missing.removeAll(foundIds);
            throw new InvalidTagIdsException(missing);
        }
        return new java.util.HashSet<>(found);
    }
}
