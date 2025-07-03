package com.pfe.DFinancialStatement.activity.repository;

import com.pfe.DFinancialStatement.activity.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
}
