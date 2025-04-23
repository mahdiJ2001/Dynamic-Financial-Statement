package com.pfe.DFinancialStatement.form_template.controller;

import com.pfe.DFinancialStatement.form_template.dto.FormTemplateDTO;
import com.pfe.DFinancialStatement.form_template.entity.FormTemplate;
import com.pfe.DFinancialStatement.form_template.service.FormTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/form-templates")
public class FormTemplateController {

    @Autowired
    private FormTemplateService formTemplateService;

    @PostMapping
    public ResponseEntity<FormTemplate> saveTemplate(@RequestBody FormTemplateDTO templateDTO) {
        FormTemplate savedTemplate = formTemplateService.saveFormTemplate(templateDTO);
        return ResponseEntity.ok(savedTemplate);
    }

    @GetMapping
    public ResponseEntity<List<FormTemplate>> getAllTemplates() {
        List<FormTemplate> templates = formTemplateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormTemplate> getTemplateById(@PathVariable Long id) {
        Optional<FormTemplate> template = formTemplateService.getTemplateById(id);
        return template.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTemplate(@PathVariable Long id) {
        boolean deleted = formTemplateService.deleteTemplate(id);
        if (deleted) {
            return ResponseEntity.ok("Template deleted successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
