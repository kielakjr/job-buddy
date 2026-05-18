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
}
