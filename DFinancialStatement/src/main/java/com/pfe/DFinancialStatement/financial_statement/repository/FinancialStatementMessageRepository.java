package com.pfe.DFinancialStatement.financial_statement.repository;

import com.pfe.DFinancialStatement.financial_statement.entity.FinancialStatementMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FinancialStatementMessageRepository extends JpaRepository<FinancialStatementMessage, Long> {
    List<FinancialStatementMessage> findByFinancialStatementIdOrderBySentAtAsc(Long financialStatementId);
}
