package com.pfe.DFinancialStatement.error_messages.repository;

import com.pfe.DFinancialStatement.error_messages.entity.ErrorMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ErrorMessageRepository extends JpaRepository<ErrorMessage, Long> {
    Optional<ErrorMessage> findByErrorCode(String errorCode);
}

