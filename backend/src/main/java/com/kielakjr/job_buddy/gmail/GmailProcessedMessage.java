package com.kielakjr.job_buddy.gmail;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;

import com.kielakjr.job_buddy.application.Application;
import com.kielakjr.job_buddy.user.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "gmail_processed_messages",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "gmail_msg_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GmailProcessedMessage {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "gmail_msg_id", nullable = false)
    private String gmailMsgId;

    @Column(name = "from_address")
    private String fromAddress;

    @Column(columnDefinition = "text")
    private String subject;

    @Column(name = "received_at")
    private OffsetDateTime receivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_to_app")
    private Application matchedToApp;

    @Column(name = "action_taken", length = 100)
    private String actionTaken;

    @CreationTimestamp
    @Column(name = "processed_at", updatable = false)
    private OffsetDateTime processedAt;
}
