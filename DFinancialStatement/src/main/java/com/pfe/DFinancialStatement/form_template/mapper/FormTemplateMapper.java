package com.pfe.DFinancialStatement.form_template.mapper;

import com.pfe.DFinancialStatement.form_template.entity.FormTemplate;
import com.pfe.DFinancialStatement.form_template.dto.FormTemplateDTO;
import org.springframework.stereotype.Component;

@Component
public class FormTemplateMapper {

    public FormTemplateDTO toDTO(FormTemplate formTemplate) {
        if (formTemplate == null) {
            return null;
        }

        FormTemplateDTO dto = new FormTemplateDTO();
        dto.setName(formTemplate.getName());
        dto.setFormStructure(formTemplate.getFormStructure());
        dto.setDateCreation(formTemplate.getDateCreation());

        return dto;
    }

    public FormTemplate toEntity(FormTemplateDTO formTemplateDTO) {
        if (formTemplateDTO == null) {
            return null;
        }

        FormTemplate formTemplate = new FormTemplate();
        formTemplate.setName(formTemplateDTO.getName());
        formTemplate.setFormStructure(formTemplateDTO.getFormStructure());
        formTemplate.setDateCreation(formTemplateDTO.getDateCreation());

        return formTemplate;
    }
}
