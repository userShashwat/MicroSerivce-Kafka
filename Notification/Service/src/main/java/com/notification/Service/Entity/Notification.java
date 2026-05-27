package com.notification.Service.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notifications")
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;
    @Column(columnDefinition = "TEXT")
    private String payload;
    @Column(nullable = false)
    private int retryCount;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.retryCount = 0;
        if (this.status == null) this.status =NotificationStatus.PENDING;
    }

}
