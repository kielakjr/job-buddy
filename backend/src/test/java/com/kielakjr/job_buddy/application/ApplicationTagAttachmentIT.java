package com.kielakjr.job_buddy.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.kielakjr.job_buddy.TestcontainersConfiguration;
import com.kielakjr.job_buddy.auth.CustomOAuth2UserService;
import com.kielakjr.job_buddy.tag.Tag;
import com.kielakjr.job_buddy.tag.TagRepository;
import com.kielakjr.job_buddy.user.User;
import com.kielakjr.job_buddy.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ApplicationTagAttachmentIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ApplicationRepository applicationRepository;

    @Autowired
    TagRepository tagRepository;

    User alice;
    User bob;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(User.builder().email("a@x.com").build());
        bob = userRepository.save(User.builder().email("b@x.com").build());
    }

    private static RequestPostProcessor as(User u) {
        return oauth2Login()
                .attributes(a -> a.put(
                        CustomOAuth2UserService.PRINCIPAL_USER_ID_ATTR,
                        u.getId().toString()));
    }

    private Application seedApp(User owner) {
        return applicationRepository.save(Application.builder()
                .user(owner)
                .company("c")
                .position("p")
                .status(ApplicationStatus.DRAFT)
                .tags(new HashSet<>())
                .build());
    }

    private Tag seedTag(User owner, String name) {
        return tagRepository.save(Tag.builder().user(owner).name(name).build());
    }

    @Nested
    class Attach {

        @Test
        void attachesTag() throws Exception {
            var app = seedApp(alice);
            var tag = seedTag(alice, "x");

            mvc.perform(post("/api/applications/{a}/tags/{t}", app.getId(), tag.getId())
                            .with(as(alice)))
                    .andExpect(status().isNoContent());

            var reloaded = applicationRepository.findById(app.getId()).orElseThrow();
            assertThat(reloaded.getTags()).extracting(Tag::getId).containsExactly(tag.getId());
        }

        @Test
        void attachIsIdempotent() throws Exception {
            var app = seedApp(alice);
            var tag = seedTag(alice, "x");

            mvc.perform(post("/api/applications/{a}/tags/{t}", app.getId(), tag.getId())
                            .with(as(alice)))
                    .andExpect(status().isNoContent());
            mvc.perform(post("/api/applications/{a}/tags/{t}", app.getId(), tag.getId())
                            .with(as(alice)))
                    .andExpect(status().isNoContent());

            var reloaded = applicationRepository.findById(app.getId()).orElseThrow();
            assertThat(reloaded.getTags()).hasSize(1);
        }

        @Test
        void returns404WhenAppBelongsToOtherUser() throws Exception {
            var app = seedApp(alice);
            var tag = seedTag(bob, "x");

            mvc.perform(post("/api/applications/{a}/tags/{t}", app.getId(), tag.getId())
                            .with(as(bob)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns404WhenTagBelongsToOtherUser() throws Exception {
            var app = seedApp(alice);
            var tag = seedTag(bob, "x");

            mvc.perform(post("/api/applications/{a}/tags/{t}", app.getId(), tag.getId())
                            .with(as(alice)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Detach {

        @Test
        void detachesTag() throws Exception {
            var tag = seedTag(alice, "x");
            var app = applicationRepository.save(Application.builder()
                    .user(alice)
                    .company("c")
                    .position("p")
                    .status(ApplicationStatus.DRAFT)
                    .tags(new HashSet<>(Set.of(tag)))
                    .build());

            mvc.perform(delete("/api/applications/{a}/tags/{t}", app.getId(), tag.getId())
                            .with(as(alice)))
                    .andExpect(status().isNoContent());

            var reloaded = applicationRepository.findById(app.getId()).orElseThrow();
            assertThat(reloaded.getTags()).isEmpty();
        }

        @Test
        void detachIsIdempotentWhenNotAttached() throws Exception {
            var app = seedApp(alice);
            var tag = seedTag(alice, "x");

            mvc.perform(delete("/api/applications/{a}/tags/{t}", app.getId(), tag.getId())
                            .with(as(alice)))
                    .andExpect(status().isNoContent());
        }

        @Test
        void returns404WhenAppBelongsToOtherUser() throws Exception {
            var app = seedApp(alice);
            var tag = seedTag(alice, "x");

            mvc.perform(delete("/api/applications/{a}/tags/{t}", app.getId(), tag.getId())
                            .with(as(bob)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void unknownIdsReturn404() throws Exception {
            mvc.perform(delete("/api/applications/{a}/tags/{t}", UUID.randomUUID(), UUID.randomUUID())
                            .with(as(alice)))
                    .andExpect(status().isNotFound());
        }
    }
}
