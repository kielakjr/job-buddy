package com.kielakjr.job_buddy.tag;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.kielakjr.job_buddy.TestcontainersConfiguration;
import com.kielakjr.job_buddy.auth.CustomOAuth2UserService;
import com.kielakjr.job_buddy.user.User;
import com.kielakjr.job_buddy.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class TagControllerIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository userRepository;

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

    private Tag seedTag(User owner, String name, String color) {
        return tagRepository.saveAndFlush(
                Tag.builder().user(owner).name(name).color(color).build());
    }

    @Nested
    class Auth {

        @Test
        void unauthenticatedRequestsAre401() throws Exception {
            mvc.perform(get("/api/tags")).andExpect(status().isUnauthorized());
            mvc.perform(post("/api/tags")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class Create {

        @Test
        void createsTagAndReturns201() throws Exception {
            var body = "{ \"name\":\"urgent\", \"color\":\"#FF0000\" }";

            mvc.perform(post("/api/tags")
                            .with(as(alice))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("urgent"))
                    .andExpect(jsonPath("$.color").value("#FF0000"));
        }

        @Test
        void returns400OnInvalidColor() throws Exception {
            var body = "{ \"name\":\"x\", \"color\":\"red\" }";
            mvc.perform(post("/api/tags")
                            .with(as(alice))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns400OnBlankName() throws Exception {
            var body = "{ \"name\":\"\" }";
            mvc.perform(post("/api/tags")
                            .with(as(alice))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns409OnDuplicateName() throws Exception {
            // Commit seed data so the DB constraint is visible to the controller's transaction
            seedTag(alice, "dup", null);
            TestTransaction.flagForCommit();
            TestTransaction.end();

            try {
                var body = "{ \"name\":\"dup\" }";
                mvc.perform(post("/api/tags")
                                .with(as(alice))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andExpect(status().isConflict());
            } finally {
                // Clean up committed data
                TestTransaction.start();
                tagRepository.deleteAll();
                userRepository.deleteAll();
                TestTransaction.flagForCommit();
                TestTransaction.end();
            }
        }
    }

    @Nested
    class List {

        @Test
        void returnsOnlyOwnTagsSortedByName() throws Exception {
            seedTag(alice, "backend", null);
            seedTag(alice, "apply", null);
            seedTag(bob, "bobs", null);

            mvc.perform(get("/api/tags").with(as(alice)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("apply"))
                    .andExpect(jsonPath("$[1].name").value("backend"));
        }
    }

    @Nested
    class Update {

        @Test
        void patchUpdatesOnlyProvidedFields() throws Exception {
            var tag = seedTag(alice, "old", "#000000");
            mvc.perform(patch("/api/tags/{id}", tag.getId())
                            .with(as(alice))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"name\":\"new\" }"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("new"))
                    .andExpect(jsonPath("$.color").value("#000000"));
        }

        @Test
        void returns400OnInvalidColor() throws Exception {
            var tag = seedTag(alice, "t", null);
            mvc.perform(patch("/api/tags/{id}", tag.getId())
                            .with(as(alice))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"color\":\"red\" }"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesTag() throws Exception {
            var tag = seedTag(alice, "t", null);
            mvc.perform(delete("/api/tags/{id}", tag.getId()).with(as(alice))).andExpect(status().isNoContent());

            mvc.perform(patch("/api/tags/{id}", tag.getId())
                            .with(as(alice))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"name\":\"x\" }"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Authorization {

        @Test
        void otherUsersTagReturns404OnPatch() throws Exception {
            var tag = seedTag(alice, "t", null);
            mvc.perform(patch("/api/tags/{id}", tag.getId())
                            .with(as(bob))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"name\":\"x\" }"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void otherUsersTagReturns404OnDelete() throws Exception {
            var tag = seedTag(alice, "t", null);
            mvc.perform(delete("/api/tags/{id}", tag.getId()).with(as(bob))).andExpect(status().isNotFound());
        }

        @Test
        void unknownIdReturns404() throws Exception {
            mvc.perform(delete("/api/tags/{id}", UUID.randomUUID()).with(as(alice)))
                    .andExpect(status().isNotFound());
        }
    }
}
