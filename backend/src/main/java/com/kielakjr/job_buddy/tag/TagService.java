package com.kielakjr.job_buddy.tag;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kielakjr.job_buddy.tag.dto.CreateTagRequest;
import com.kielakjr.job_buddy.tag.dto.TagResponse;
import com.kielakjr.job_buddy.tag.dto.UpdateTagRequest;
import com.kielakjr.job_buddy.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository repo;

    @Transactional
    public TagResponse create(User user, CreateTagRequest req) {
        var tag = Tag.builder().user(user).name(req.name()).color(req.color()).build();
        return TagResponse.from(repo.save(tag));
    }

    @Transactional(readOnly = true)
    public List<TagResponse> list(User user) {
        return repo.findAllByUser_IdOrderByNameAsc(user.getId()).stream()
                .map(TagResponse::from)
                .toList();
    }

    @Transactional
    public TagResponse update(User user, UUID id, UpdateTagRequest req) {
        var tag = loadOwned(user, id);
        if (req.name() != null) {
            if (req.name().isBlank()) {
                throw new IllegalArgumentException("name must not be blank");
            }
            tag.setName(req.name());
        }
        if (req.color() != null) {
            tag.setColor(req.color());
        }
        return TagResponse.from(repo.save(tag));
    }

    @Transactional
    public void delete(User user, UUID id) {
        long removed = repo.deleteByIdAndUser_Id(id, user.getId());
        if (removed == 0) {
            throw new TagNotFoundException(id);
        }
    }

    private Tag loadOwned(User user, UUID id) {
        return repo.findByIdAndUser_Id(id, user.getId()).orElseThrow(() -> new TagNotFoundException(id));
    }
}
