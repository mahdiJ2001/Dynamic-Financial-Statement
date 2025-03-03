package com.pfe.DFinancialStatement.financial_statement.repository;

import com.pfe.DFinancialStatement.financial_statement.entity.FinancialStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialStatementRepository extends JpaRepository<FinancialStatement, Long> {
}
