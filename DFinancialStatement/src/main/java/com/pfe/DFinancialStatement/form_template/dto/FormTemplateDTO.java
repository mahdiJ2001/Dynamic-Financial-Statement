package com.pfe.DFinancialStatement.form_template.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FormTemplateDTO {
    private String name;
    private String formStructure;
    private LocalDateTime dateCreation;
}
