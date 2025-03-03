package com.pfe.DFinancialStatement.form_template.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "form_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "form_structure", columnDefinition = "text")
    private String formStructure;

    @CreationTimestamp
    @Column(name = "datecreation", updatable = false)
    private LocalDateTime dateCreation;
}
