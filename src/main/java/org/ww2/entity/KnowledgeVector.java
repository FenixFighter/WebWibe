package org.ww2.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "knowledge_vectors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeVector {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "question", columnDefinition = "TEXT")
    private String question;
    
    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;
    
    @Column(name = "category")
    private String category;
    
    @Column(name = "embedding", nullable = true)
    private String embedding; // JSON string representation of vector
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }
}
