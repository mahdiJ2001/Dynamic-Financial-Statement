package com.pfe.DFinancialStatement.error_messages.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "error_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String errorCode;

    @Column(nullable = false)
    private String customMessage;


}

