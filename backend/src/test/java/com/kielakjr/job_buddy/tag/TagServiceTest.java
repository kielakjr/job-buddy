package com.kielakjr.job_buddy.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kielakjr.job_buddy.tag.dto.CreateTagRequest;
import com.kielakjr.job_buddy.tag.dto.UpdateTagRequest;
import com.kielakjr.job_buddy.user.User;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    TagRepository repo;

    @InjectMocks
    TagService service;

    User alice;

    @BeforeEach
    void setUp() {
        alice = User.builder().id(UUID.randomUUID()).email("alice@x.com").build();
    }

    @Nested
    class Create {

        @Test
        void savesTagWithCurrentUserAndReturnsResponse() {
            when(repo.save(any(Tag.class))).thenAnswer(inv -> {
                Tag t = inv.getArgument(0);
                t.setId(UUID.randomUUID());
                return t;
            });

            var resp = service.create(alice, new CreateTagRequest("urgent", "#FF0000"));

            assertThat(resp.name()).isEqualTo("urgent");
            assertThat(resp.color()).isEqualTo("#FF0000");
            assertThat(resp.id()).isNotNull();
        }
    }

    @Nested
    class List {

        @Test
        void returnsOnlyCurrentUsersTagsSortedByName() {
            var t1 = Tag.builder()
                    .id(UUID.randomUUID())
                    .user(alice)
                    .name("backend")
                    .build();
            var t2 = Tag.builder()
                    .id(UUID.randomUUID())
                    .user(alice)
                    .name("apply")
                    .color("#000000")
                    .build();
            when(repo.findAllByUser_IdOrderByNameAsc(alice.getId())).thenReturn(java.util.List.of(t2, t1));

            var result = service.list(alice);

            assertThat(result).extracting("name").containsExactly("apply", "backend");
        }
    }

    @Nested
    class Update {

        @Test
        void appliesPartialUpdate() {
            var id = UUID.randomUUID();
            var existing = Tag.builder()
                    .id(id)
                    .user(alice)
                    .name("old")
                    .color("#000000")
                    .build();
            when(repo.findByIdAndUser_Id(id, alice.getId())).thenReturn(Optional.of(existing));
            when(repo.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

            var resp = service.update(alice, id, new UpdateTagRequest("new", null));

            assertThat(resp.name()).isEqualTo("new");
            assertThat(resp.color()).isEqualTo("#000000");
        }

        @Test
        void rejectsBlankName() {
            var id = UUID.randomUUID();
            var existing = Tag.builder().id(id).user(alice).name("old").build();
            when(repo.findByIdAndUser_Id(id, alice.getId())).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.update(alice, id, new UpdateTagRequest("  ", null)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void throwsTagNotFoundForOtherUsersTag() {
            var id = UUID.randomUUID();
            when(repo.findByIdAndUser_Id(id, alice.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(alice, id, new UpdateTagRequest("x", null)))
                    .isInstanceOf(TagNotFoundException.class);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesWhenOwned() {
            var id = UUID.randomUUID();
            when(repo.deleteByIdAndUser_Id(id, alice.getId())).thenReturn(1L);
            service.delete(alice, id);
        }

        @Test
        void throwsTagNotFoundForOtherUsersTag() {
            var id = UUID.randomUUID();
            when(repo.deleteByIdAndUser_Id(id, alice.getId())).thenReturn(0L);

            assertThatThrownBy(() -> service.delete(alice, id)).isInstanceOf(TagNotFoundException.class);
        }
    }
}
