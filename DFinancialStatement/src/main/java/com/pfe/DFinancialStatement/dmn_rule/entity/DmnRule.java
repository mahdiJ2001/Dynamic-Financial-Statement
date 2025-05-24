package com.pfe.DFinancialStatement.dmn_rule.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pfe.DFinancialStatement.form_template.entity.FormTemplate;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "regles_dmn")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DmnRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_key", nullable = false, unique = true)
    private String ruleKey;

    @Column(name = "rule_content", columnDefinition = "text", nullable = false)
    private String ruleContent;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Column(name = "rule_dtos", columnDefinition = "text")
    private String ruleDtosJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private FormTemplate formTemplate;

}
