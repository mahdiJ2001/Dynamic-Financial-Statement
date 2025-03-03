package com.pfe.DFinancialStatement.dmn_rule.repository;

import com.pfe.DFinancialStatement.dmn_rule.entity.DmnRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DmnRuleRepository extends JpaRepository<DmnRule, Long> {
    Optional<DmnRule> findByRuleKey(String ruleKey);
}
