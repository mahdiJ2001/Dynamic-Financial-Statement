package com.pfe.DFinancialStatement.dmn_rule.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
