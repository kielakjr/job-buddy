package com.kielakjr.job_buddy.gmail;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import com.kielakjr.job_buddy.user.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gmail_integrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GmailIntegration {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "encrypted_refresh_token", nullable = false, columnDefinition = "text")
    private String encryptedRefreshToken;

    @Column(name = "history_id", length = 50)
    private String historyId;

    @Column(name = "watch_expires_at")
    private OffsetDateTime watchExpiresAt;

    @Column(nullable = false)
    private Boolean enabled;

    @CreationTimestamp
    @Column(name = "connected_at", updatable = false)
    private OffsetDateTime connectedAt;

    @Column(name = "last_sync_at")
    private OffsetDateTime lastSyncAt;
}
