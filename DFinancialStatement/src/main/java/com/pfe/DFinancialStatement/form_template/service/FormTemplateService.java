package com.pfe.DFinancialStatement.form_template.service;

import com.pfe.DFinancialStatement.activity.enums.ActionType;
import com.pfe.DFinancialStatement.activity.service.ActivityLogService;
import com.pfe.DFinancialStatement.auth.entity.User;
import com.pfe.DFinancialStatement.auth.service.AuthService;
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

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private AuthService authService;


    public FormTemplate saveFormTemplate(FormTemplateDTO templateDTO) {
        FormTemplate formTemplate = new FormTemplate();
        formTemplate.setName(templateDTO.getName());
        formTemplate.setFormStructure(templateDTO.getFormStructure());

        FormTemplate saved = formTemplateRepository.save(formTemplate);

        // Get current user
        User currentUser = authService.getCurrentUser();

        // Log activity
        activityLogService.log(
                ActionType.CREATE_TEMPLATE,
                "User " + currentUser.getUsername() + " created template: " + saved.getName()
        );

        return saved;
    }


    public List<FormTemplate> getAllTemplates() {
        return formTemplateRepository.findAll();
    }

    public Optional<FormTemplate> getTemplateById(Long id) {
        return formTemplateRepository.findById(id);
    }

    public boolean deleteTemplate(Long id) {
        Optional<FormTemplate> templateOpt = formTemplateRepository.findById(id);
        if (templateOpt.isPresent()) {
            formTemplateRepository.deleteById(id);

            User currentUser = authService.getCurrentUser();
            activityLogService.log(
                    ActionType.DELETE_TEMPLATE,
                    "User " + currentUser.getUsername() + " deleted template: " + templateOpt.get().getName()
            );

            return true;
        }
        return false;
    }

}
