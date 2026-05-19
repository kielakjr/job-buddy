package com.kielakjr.job_buddy.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kielakjr.job_buddy.application.dto.CreateApplicationRequest;
import com.kielakjr.job_buddy.application.dto.UpdateApplicationRequest;
import com.kielakjr.job_buddy.user.User;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    ApplicationRepository applicationRepository;

    @Mock
    ApplicationEventRepository eventRepository;

    @InjectMocks
    ApplicationService service;

    User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("u@x.com").build();
    }

    private CreateApplicationRequest fullCreate() {
        return new CreateApplicationRequest(
                "ACME",
                "Backend Engineer",
                "Warsaw",
                true,
                "linkedin",
                "https://acme/jobs/1",
                15000,
                25000,
                "PLN",
                LocalDate.of(2026, 5, 1),
                "looks good",
                null);
    }

    private Application existing(UUID id, ApplicationStatus status) {
        return Application.builder()
                .id(id)
                .user(user)
                .company("ACME")
                .position("Backend Engineer")
                .status(status)
                .build();
    }

    @Nested
    class Create {

        @Test
        void persistsWithCurrentUserAndDefaultsToDraft() {
            when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
                Application a = inv.getArgument(0);
                a.setId(UUID.randomUUID());
                return a;
            });

            var resp = service.create(user, fullCreate());

            var captor = ArgumentCaptor.forClass(Application.class);
            verify(applicationRepository).save(captor.capture());
            var saved = captor.getValue();
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.getCompany()).isEqualTo("ACME");
            assertThat(saved.getPosition()).isEqualTo("Backend Engineer");
            assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
            assertThat(saved.getSalaryMin()).isEqualTo(15000);
            assertThat(saved.getAppliedAt()).isEqualTo(LocalDate.of(2026, 5, 1));
            assertThat(resp.company()).isEqualTo("ACME");
            assertThat(resp.status()).isEqualTo(ApplicationStatus.DRAFT);
        }
    }

    @Nested
    class List_ {

        @Test
        void returnsOnlyCurrentUsersApplications() {
            var a1 = existing(UUID.randomUUID(), ApplicationStatus.DRAFT);
            var a2 = existing(UUID.randomUUID(), ApplicationStatus.APPLIED);
            when(applicationRepository.findAllByUser_IdOrderByCreatedAtDesc(user.getId()))
                    .thenReturn(List.of(a1, a2));

            var result = service.list(user);

            assertThat(result).hasSize(2);
            assertThat(result).extracting("status").containsExactly(ApplicationStatus.DRAFT, ApplicationStatus.APPLIED);
        }
    }

    @Nested
    class Get {

        @Test
        void returnsApplication_whenOwned() {
            var id = UUID.randomUUID();
            when(applicationRepository.findByIdAndUser_Id(id, user.getId()))
                    .thenReturn(Optional.of(existing(id, ApplicationStatus.APPLIED)));

            var resp = service.get(user, id);

            assertThat(resp.id()).isEqualTo(id);
            assertThat(resp.status()).isEqualTo(ApplicationStatus.APPLIED);
        }

        @Test
        void throwsNotFound_whenMissingOrNotOwned() {
            var id = UUID.randomUUID();
            when(applicationRepository.findByIdAndUser_Id(id, user.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(user, id)).isInstanceOf(ApplicationNotFoundException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void mutatesAllowedFields_leavesStatusAlone() {
            var id = UUID.randomUUID();
            var app = existing(id, ApplicationStatus.APPLIED);
            when(applicationRepository.findByIdAndUser_Id(id, user.getId())).thenReturn(Optional.of(app));
            when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

            var req = new UpdateApplicationRequest(
                    "NewCo", null, "Remote", null, null, null, null, null, null, null, "notes2", null);
            var resp = service.update(user, id, req);

            assertThat(resp.company()).isEqualTo("NewCo");
            assertThat(resp.position()).isEqualTo("Backend Engineer");
            assertThat(resp.location()).isEqualTo("Remote");
            assertThat(resp.notes()).isEqualTo("notes2");
            assertThat(resp.status()).isEqualTo(ApplicationStatus.APPLIED);
            verify(eventRepository, never()).save(any());
        }

        @Test
        void ignoresAllNullFields() {
            var id = UUID.randomUUID();
            var app = existing(id, ApplicationStatus.APPLIED);
            when(applicationRepository.findByIdAndUser_Id(id, user.getId())).thenReturn(Optional.of(app));
            when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

            var req = new UpdateApplicationRequest(
                    null, null, null, null, null, null, null, null, null, null, null, null);
            var resp = service.update(user, id, req);

            assertThat(resp.company()).isEqualTo("ACME");
            assertThat(resp.position()).isEqualTo("Backend Engineer");
        }

        @Test
        void throwsNotFound_whenMissingOrNotOwned() {
            var id = UUID.randomUUID();
            when(applicationRepository.findByIdAndUser_Id(id, user.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(
                            user,
                            id,
                            new UpdateApplicationRequest(
                                    null, null, null, null, null, null, null, null, null, null, null, null)))
                    .isInstanceOf(ApplicationNotFoundException.class);
        }
    }

    @Nested
    class ChangeStatus {

        @Test
        void writesEventAndSetsNewStatus() {
            var id = UUID.randomUUID();
            var app = existing(id, ApplicationStatus.DRAFT);
            when(applicationRepository.findByIdAndUser_Id(id, user.getId())).thenReturn(Optional.of(app));
            when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

            var resp = service.changeStatus(user, id, ApplicationStatus.APPLIED);

            assertThat(resp.status()).isEqualTo(ApplicationStatus.APPLIED);
            assertThat(app.getStatus()).isEqualTo(ApplicationStatus.APPLIED);

            var captor = ArgumentCaptor.forClass(ApplicationEvent.class);
            verify(eventRepository).save(captor.capture());
            var event = captor.getValue();
            assertThat(event.getApplication()).isSameAs(app);
            assertThat(event.getEventType()).isEqualTo("STATUS_CHANGED");
            assertThat(event.getMetadata()).containsEntry("from", "DRAFT").containsEntry("to", "APPLIED");
        }

        @Test
        void isNoOp_whenStatusUnchanged() {
            var id = UUID.randomUUID();
            var app = existing(id, ApplicationStatus.APPLIED);
            when(applicationRepository.findByIdAndUser_Id(id, user.getId())).thenReturn(Optional.of(app));

            var resp = service.changeStatus(user, id, ApplicationStatus.APPLIED);

            assertThat(resp.status()).isEqualTo(ApplicationStatus.APPLIED);
            verify(applicationRepository, never()).save(any());
            verify(eventRepository, never()).save(any());
        }

        @Test
        void throwsNotFound_whenMissingOrNotOwned() {
            var id = UUID.randomUUID();
            when(applicationRepository.findByIdAndUser_Id(id, user.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.changeStatus(user, id, ApplicationStatus.APPLIED))
                    .isInstanceOf(ApplicationNotFoundException.class);
        }
    }

    @Nested
    class Delete {

        @Test
        void removes_whenOwned() {
            var id = UUID.randomUUID();
            when(applicationRepository.deleteByIdAndUser_Id(id, user.getId())).thenReturn(1L);

            service.delete(user, id);

            verify(applicationRepository).deleteByIdAndUser_Id(id, user.getId());
        }

        @Test
        void throwsNotFound_whenMissingOrNotOwned() {
            var id = UUID.randomUUID();
            when(applicationRepository.deleteByIdAndUser_Id(id, user.getId())).thenReturn(0L);

            assertThatThrownBy(() -> service.delete(user, id)).isInstanceOf(ApplicationNotFoundException.class);
        }
    }
}
