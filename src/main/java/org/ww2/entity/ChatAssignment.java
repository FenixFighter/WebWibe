package org.ww2.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatAssignment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "chat_id", nullable = false)
    private String chatId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User assignedUser;
    
    @Column(name = "user_id", insertable = false, updatable = false)
    private Long assignedUserId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AssignmentStatus status;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
    }
    
    public enum AssignmentStatus {
        PENDING, ACTIVE, RESOLVED
    }
}
