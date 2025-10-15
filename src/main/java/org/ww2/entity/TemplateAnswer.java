package org.ww2.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "template_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateAnswer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "category", nullable = false)
    private String category;
    
    @Column(name = "subcategory")
    private String subcategory;
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
}

