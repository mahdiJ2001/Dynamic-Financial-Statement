package com.pfe.DFinancialStatement.form_template.service;

import com.pfe.DFinancialStatement.form_template.dto.FormTemplateDTO;
import com.pfe.DFinancialStatement.form_template.entity.FormTemplate;
import com.pfe.DFinancialStatement.form_template.repository.FormTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class FormTemplateService {

    @Autowired
    private FormTemplateRepository formTemplateRepository;

    public FormTemplate saveFormTemplate(FormTemplateDTO templateDTO) {
        FormTemplate formTemplate = new FormTemplate();
        formTemplate.setName(templateDTO.getName());
        formTemplate.setFormStructure(templateDTO.getFormStructure());

        return formTemplateRepository.save(formTemplate);
    }

    public List<FormTemplate> getAllTemplates() {
        return formTemplateRepository.findAll();
    }

    public Optional<FormTemplate> getTemplateById(Long id) {
        return formTemplateRepository.findById(id);
    }

    public boolean deleteTemplate(Long id) {
        if (formTemplateRepository.existsById(id)) {
            formTemplateRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
