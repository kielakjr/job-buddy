package com.kielakjr.job_buddy.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    @Nested
    class GetById {

        @Test
        void returnsUser_whenFound() {
            var id = UUID.randomUUID();
            var user = User.builder().id(id).email("a@b.com").build();
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            assertThat(userService.getById(id)).isSameAs(user);
        }

        @Test
        void throws_whenMissing() {
            var id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getById(id))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    @Nested
    class UpsertFromOAuth {

        @Test
        void throws_whenEmailMissing() {
            var profile = new OAuthProfile("google", "g-1", null, "Name", null);

            assertThatThrownBy(() -> userService.upsertFromOAuth(profile)).isInstanceOf(IllegalArgumentException.class);
            verify(userRepository, never()).save(any());
        }

        @Test
        void matchesByGoogleId_andUpdatesProfile() {
            var existing = User.builder()
                    .id(UUID.randomUUID())
                    .email("old@x.com")
                    .googleId("g-1")
                    .build();
            when(userRepository.findByGoogleId("g-1")).thenReturn(Optional.of(existing));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            var profile = new OAuthProfile("google", "g-1", "new@x.com", "New Name", "http://pic");
            var saved = userService.upsertFromOAuth(profile);

            assertThat(saved.getEmail()).isEqualTo("new@x.com");
            assertThat(saved.getName()).isEqualTo("New Name");
            assertThat(saved.getAvatarUrl()).isEqualTo("http://pic");
            assertThat(saved.getGoogleId()).isEqualTo("g-1");
        }

        @Test
        void fallsBackToEmail_whenProviderIdUnknown() {
            var existing = User.builder().id(UUID.randomUUID()).email("a@b.com").build();
            when(userRepository.findByGoogleId("g-2")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(existing));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            var profile = new OAuthProfile("google", "g-2", "a@b.com", "A", null);
            var saved = userService.upsertFromOAuth(profile);

            assertThat(saved.getGoogleId()).isEqualTo("g-2");
            assertThat(saved.getEmail()).isEqualTo("a@b.com");
        }

        @Test
        void createsNewUser_whenNoMatch() {
            when(userRepository.findByLinkedinId("l-1")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("new@x.com")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            var profile = new OAuthProfile("linkedin", "l-1", "new@x.com", "N", "pic");
            var saved = userService.upsertFromOAuth(profile);

            assertThat(saved.getEmail()).isEqualTo("new@x.com");
            assertThat(saved.getLinkedinId()).isEqualTo("l-1");
            assertThat(saved.getGoogleId()).isNull();
            assertThat(saved.getName()).isEqualTo("N");
        }

        @Test
        void unknownProvider_doesNotSetEitherProviderId() {
            when(userRepository.findByEmail("z@x.com")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            var profile = new OAuthProfile("github", "gh-1", "z@x.com", "Z", null);
            var saved = userService.upsertFromOAuth(profile);

            assertThat(saved.getGoogleId()).isNull();
            assertThat(saved.getLinkedinId()).isNull();
            assertThat(saved.getEmail()).isEqualTo("z@x.com");
        }
    }
}
