package com.kielakjr.job_buddy.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
class ApplicationControllerIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ApplicationRepository applicationRepository;

    @Autowired
    ApplicationEventRepository eventRepository;

    @Autowired
    TagRepository tagRepository;

    User alice;
    User bob;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(User.builder().email("alice@x.com").build());
        bob = userRepository.save(User.builder().email("bob@x.com").build());
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
                .company("ACME")
                .position("Eng")
                .status(ApplicationStatus.DRAFT)
                .build());
    }

    @Nested
    class Auth {

        @Test
        void unauthenticatedRequestsAre401() throws Exception {
            mvc.perform(get("/api/applications")).andExpect(status().isUnauthorized());
            mvc.perform(post("/api/applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class Create {

        @Test
        void createsApplication_andListIncludesIt() throws Exception {
            var body =
                    """
                    { "company":"ACME","position":"Backend Engineer","remote":true,"salaryCurrency":"PLN" }
                    """;

            mvc.perform(post("/api/applications")
                            .with(as(alice))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.company").value("ACME"))
                    .andExpect(jsonPath("$.status").value("DRAFT"));

            mvc.perform(get("/api/applications").with(as(alice)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].company").value("ACME"));
        }

        @Test
        void returns400_whenRequiredFieldsMissing() throws Exception {
            var body = "{ \"position\":\"Eng\" }"; // company missing

            mvc.perform(post("/api/applications")
                            .with(as(alice))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Get {

        @Test
        void returns404_whenApplicationBelongsToAnotherUser() throws Exception {
            var alicesApp = seedApp(alice);

            mvc.perform(get("/api/applications/{id}", alicesApp.getId()).with(as(bob)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns404_whenUnknownId() throws Exception {
            mvc.perform(get("/api/applications/{id}", UUID.randomUUID()).with(as(alice)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Update {

        @Test
        void patchUpdatesOnlyProvidedFields() throws Exception {
            var app = seedApp(alice);
            var body = "{ \"company\":\"NewCo\" }";

            mvc.perform(patch("/api/applications/{id}", app.getId())
                            .with(as(alice))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.company").value("NewCo"))
                    .andExpect(jsonPath("$.position").value("Eng"))
                    .andExpect(jsonPath("$.status").value("DRAFT"));
        }
    }

    @Nested
    class ChangeStatus {

        @Test
        void putStatusWritesEventRow() throws Exception {
            var app = seedApp(alice);
            var body = "{ \"status\":\"APPLIED\" }";

            mvc.perform(put("/api/applications/{id}/status", app.getId())
                            .with(as(alice))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPLIED"));

            var events = eventRepository.findAllByApplication_IdOrderByOccurredAtDesc(app.getId());
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getEventType()).isEqualTo("STATUS_CHANGED");
            assertThat(events.get(0).getMetadata())
                    .containsEntry("from", "DRAFT")
                    .containsEntry("to", "APPLIED");
        }
    }

    @Nested
    class Delete {

        @Test
        void removesApplication() throws Exception {
            var app = seedApp(alice);

            mvc.perform(delete("/api/applications/{id}", app.getId()).with(as(alice)))
                    .andExpect(status().isNoContent());

            mvc.perform(get("/api/applications/{id}", app.getId()).with(as(alice)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns404_whenApplicationBelongsToAnotherUser() throws Exception {
            var alicesApp = seedApp(alice);

            mvc.perform(delete("/api/applications/{id}", alicesApp.getId()).with(as(bob)))
                    .andExpect(status().isNotFound());

            mvc.perform(get("/api/applications/{id}", alicesApp.getId()).with(as(alice)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class UpdateWithTags {

        @Test
        void omittedTagIdsLeavesTagsUnchanged() throws Exception {
            var tag = tagRepository.save(Tag.builder().user(alice).name("x").build());
            var app = applicationRepository.save(Application.builder()
                    .user(alice)
                    .company("c")
                    .position("p")
                    .status(ApplicationStatus.DRAFT)
                    .tags(new java.util.HashSet<>(java.util.Set.of(tag)))
                    .build());

            mvc.perform(patch("/api/applications/{id}", app.getId())
                            .with(as(alice))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"company\":\"newco\" }"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tags.length()").value(1))
                    .andExpect(jsonPath("$.tags[0].name").value("x"));
        }

        @Test
        void emptyTagIdsClearsAll() throws Exception {
            var tag = tagRepository.save(Tag.builder().user(alice).name("x").build());
            var app = applicationRepository.save(Application.builder()
                    .user(alice)
                    .company("c")
                    .position("p")
                    .status(ApplicationStatus.DRAFT)
                    .tags(new java.util.HashSet<>(java.util.Set.of(tag)))
                    .build());

            mvc.perform(patch("/api/applications/{id}", app.getId())
                            .with(as(alice))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"tagIds\":[] }"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tags.length()").value(0));
        }

        @Test
        void nonEmptyTagIdsReplacesFullSet() throws Exception {
            var t1 = tagRepository.save(Tag.builder().user(alice).name("old").build());
            var t2 = tagRepository.save(Tag.builder().user(alice).name("new").build());
            var app = applicationRepository.save(Application.builder()
                    .user(alice)
                    .company("c")
                    .position("p")
                    .status(ApplicationStatus.DRAFT)
                    .tags(new java.util.HashSet<>(java.util.Set.of(t1)))
                    .build());

            mvc.perform(patch("/api/applications/{id}", app.getId())
                            .with(as(alice))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"tagIds\":[\"%s\"] }".formatted(t2.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tags.length()").value(1))
                    .andExpect(jsonPath("$.tags[0].name").value("new"));
        }

        @Test
        void unknownTagIdReturns400() throws Exception {
            var app = applicationRepository.save(Application.builder()
                    .user(alice)
                    .company("c")
                    .position("p")
                    .status(ApplicationStatus.DRAFT)
                    .build());

            mvc.perform(patch("/api/applications/{id}", app.getId())
                            .with(as(alice))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"tagIds\":[\"%s\"] }".formatted(java.util.UUID.randomUUID())))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class CreateWithTags {

        @Test
        void createAttachesTagsByIds() throws Exception {
            var t1 = tagRepository.save(Tag.builder().user(alice).name("urgent").build());
            var t2 =
                    tagRepository.save(Tag.builder().user(alice).name("backend").build());

            var body =
                    """
                    { "company":"ACME","position":"Eng","tagIds":["%s","%s"] }
                    """
                            .formatted(t1.getId(), t2.getId());

            mvc.perform(post("/api/applications")
                            .with(as(alice))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tags.length()").value(2))
                    .andExpect(jsonPath("$.tags[0].name").value("backend"))
                    .andExpect(jsonPath("$.tags[1].name").value("urgent"));
        }

        @Test
        void returns400WhenTagIdUnknown() throws Exception {
            var body =
                    """
                    { "company":"ACME","position":"Eng","tagIds":["%s"] }
                    """
                            .formatted(java.util.UUID.randomUUID());

            mvc.perform(post("/api/applications")
                            .with(as(alice))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns400WhenTagBelongsToOtherUser() throws Exception {
            var bobsTag =
                    tagRepository.save(Tag.builder().user(bob).name("bobs").build());

            var body =
                    """
                    { "company":"ACME","position":"Eng","tagIds":["%s"] }
                    """
                            .formatted(bobsTag.getId());

            mvc.perform(post("/api/applications")
                            .with(as(alice))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }
}
