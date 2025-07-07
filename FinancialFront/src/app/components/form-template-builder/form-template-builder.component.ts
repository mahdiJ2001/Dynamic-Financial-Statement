import { Component, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FormTemplateService } from '../../services/form-template.service';
import { Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import Swal from 'sweetalert2';

export interface FinancialField {
  label: string;
  sectionType: 'actif' | 'passif';
  inputType: 'text';
}

export interface Section {
  name: string;
  type: 'actif' | 'passif';
  fields: FinancialField[];
}

@Component({
  selector: 'app-form-template-builder',
  templateUrl: './form-template-builder.component.html',
  styleUrls: ['./form-template-builder.component.css'], standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TranslateModule,
  ],
  encapsulation: ViewEncapsulation.None
})


export class FormTemplateBuilderComponent {

  jsonText: string = '';

  sectionNameMinLength: number = 3;
  sectionNameMaxLength: number = 30;
  sectionNamePattern: RegExp = /^[A-Za-zÀ-ÖØ-öø-ÿ\s']+$/;

  fieldLabelMinLength: number = 3;
  fieldLabelMaxLength: number = 50;

  fieldLabelPattern: RegExp = /^[A-Za-zÀ-ÖØ-öø-ÿ0-9\s']+$/;

  actifSections: Section[] = [
    { name: 'Immobilisations corporelles', type: 'actif', fields: [] },
    { name: 'Immobilisations incorporelles', type: 'actif', fields: [] },
    { name: 'Stocks', type: 'actif', fields: [] },
    { name: 'Créances clients', type: 'actif', fields: [] },
    { name: 'Disponibilités', type: 'actif', fields: [] }
  ];

  passifSections: Section[] = [
    { name: 'Capitaux propres', type: 'passif', fields: [] },
    { name: 'Dettes', type: 'passif', fields: [] }
  ];

  templateName: string = '';

  constructor(
    private formTemplateService: FormTemplateService,
    private router: Router,
    private translate: TranslateService
  ) { }


  addNewSection(type: 'actif' | 'passif') {
    Swal.fire({
      title: this.translate.instant('templateBuilder.alerts.section.newTitle', { type }),
      input: 'text',
      inputPlaceholder: this.translate.instant('templateBuilder.alerts.section.inputPlaceholder'),
      showCancelButton: true,
      confirmButtonText: this.translate.instant('templateBuilder.alerts.buttons.add'),
      cancelButtonText: this.translate.instant('templateBuilder.alerts.buttons.cancel'),
      inputValidator: (value) => {
        if (!value || value.trim() === '') {
          return this.translate.instant('templateBuilder.alerts.section.nameRequired');
        }

        const trimmedName = value.trim();

        if (trimmedName.length < this.sectionNameMinLength || trimmedName.length > this.sectionNameMaxLength) {
          return this.translate.instant('templateBuilder.alerts.section.nameLengthError', {
            min: this.sectionNameMinLength,
            max: this.sectionNameMaxLength
          });
        }

        if (!this.sectionNamePattern.test(trimmedName)) {
          return this.translate.instant('templateBuilder.alerts.section.nameInvalidChars');
        }

        const existingSections = type === 'actif' ? this.actifSections : this.passifSections;
        if (existingSections.some(s => s.name.toLowerCase() === trimmedName.toLowerCase())) {
          return this.translate.instant('templateBuilder.alerts.section.nameExists');
        }

        return null;
      }
    }).then(result => {
      if (result.isConfirmed && result.value) {
        const trimmedName = result.value.trim();
        const newSection: Section = { name: trimmedName, type: type, fields: [] };
        const targetList = type === 'actif' ? this.actifSections : this.passifSections;
        targetList.push(newSection);

        Swal.fire({
          icon: 'success',
          title: this.translate.instant('templateBuilder.alerts.section.addSuccess', { name: trimmedName }),
          text: this.translate.instant('templateBuilder.alerts.section.addSuccess', { name: trimmedName }),
          confirmButtonColor: '#28a745'
        });
      }
    });
  }


  removeSection(type: 'actif' | 'passif', index: number) {
    if (type === 'actif') {
      this.actifSections.splice(index, 1);
    } else {
      this.passifSections.splice(index, 1);
    }
  }

  editSectionName(section: Section) {
    Swal.fire({
      title: this.translate.instant('templateBuilder.alerts.section.editTitle'),
      input: 'text',
      inputValue: section.name,
      inputPlaceholder: this.translate.instant('templateBuilder.alerts.section.editPlaceholder'),
      showCancelButton: true,
      confirmButtonText: this.translate.instant('templateBuilder.alerts.buttons.edit'),
      cancelButtonText: this.translate.instant('templateBuilder.alerts.buttons.cancel'),
      inputValidator: (value) => {
        if (!value || value.trim() === '') {
          return this.translate.instant('templateBuilder.alerts.section.nameRequired');
        }

        const trimmedName = value.trim();

        if (trimmedName.length < this.sectionNameMinLength || trimmedName.length > this.sectionNameMaxLength) {
          return this.translate.instant('templateBuilder.alerts.section.nameLengthError', {
            min: this.sectionNameMinLength,
            max: this.sectionNameMaxLength
          });
        }

        if (!this.sectionNamePattern.test(trimmedName)) {
          return this.translate.instant('templateBuilder.alerts.section.nameInvalidChars');
        }

        const existingSections = section.type === 'actif' ? this.actifSections : this.passifSections;
        if (existingSections.some(s => s !== section && s.name.toLowerCase() === trimmedName.toLowerCase())) {
          return this.translate.instant('templateBuilder.alerts.section.editExistingName');
        }

        return null;
      }
    }).then(result => {
      if (result.isConfirmed && result.value) {
        section.name = result.value.trim();
        Swal.fire({
          icon: 'success',
          title: this.translate.instant('templateBuilder.alerts.section.editSuccess'),
          text: this.translate.instant('templateBuilder.alerts.section.editSuccess'),
          confirmButtonColor: '#28a745'
        });
      }
    });
  }

  removeField(section: Section, field: FinancialField) {
    section.fields = section.fields.filter(f => f !== field);
  }

  addFieldFromInput(section: Section, label: string) {
    if (!label || label.trim() === '') return;

    const trimmedLabel = label.trim();

    if (trimmedLabel.length < this.fieldLabelMinLength || trimmedLabel.length > this.fieldLabelMaxLength) {
      Swal.fire({
        icon: 'warning',
        title: this.translate.instant('templateBuilder.alerts.field.invalidLength', {
          min: this.fieldLabelMinLength,
          max: this.fieldLabelMaxLength
        }),
        text: this.translate.instant('templateBuilder.alerts.field.invalidLength', {
          min: this.fieldLabelMinLength,
          max: this.fieldLabelMaxLength
        }),
        confirmButtonColor: '#f0ad4e'
      });
      return;
    }

    if (!this.fieldLabelPattern.test(trimmedLabel)) {
      Swal.fire({
        icon: 'error',
        title: this.translate.instant('templateBuilder.alerts.field.invalidChars'),
        text: this.translate.instant('templateBuilder.alerts.field.invalidChars'),
        confirmButtonColor: '#d33'
      });
      return;
    }

    const newField: FinancialField = {
      label: trimmedLabel,
      sectionType: section.type,
      inputType: 'text'
    };

    section.fields.push(newField);

  }


  editFieldLabel(field: FinancialField) {
    Swal.fire({
      title: this.translate.instant('templateBuilder.alerts.field.editTitle'),
      input: 'text',
      inputValue: field.label,
      inputPlaceholder: this.translate.instant('templateBuilder.alerts.field.editPlaceholder'),
      showCancelButton: true,
      confirmButtonText: this.translate.instant('templateBuilder.alerts.buttons.edit'),
      cancelButtonText: this.translate.instant('templateBuilder.alerts.buttons.cancel'),
      inputValidator: (value) => {
        if (!value || value.trim() === '') {
          return this.translate.instant('templateBuilder.alerts.field.labelRequired');
        }

        const trimmedLabel = value.trim();

        if (trimmedLabel.length < this.fieldLabelMinLength || trimmedLabel.length > this.fieldLabelMaxLength) {
          return this.translate.instant('templateBuilder.alerts.field.invalidLength', {
            min: this.fieldLabelMinLength,
            max: this.fieldLabelMaxLength
          });
        }

        if (!this.fieldLabelPattern.test(trimmedLabel)) {
          return this.translate.instant('templateBuilder.alerts.field.invalidChars');
        }

        return null;
      }
    }).then(result => {
      if (result.isConfirmed && result.value) {
        field.label = result.value.trim();
        Swal.fire({
          icon: 'success',
          title: this.translate.instant('templateBuilder.alerts.field.editSuccess'),
          text: this.translate.instant('templateBuilder.alerts.field.editSuccess'),
          confirmButtonColor: '#28a745'
        });
      }
    });
  }


  saveTemplate() {
    const actifEmpty = this.actifSections.every(section => section.fields.length === 0);
    const passifEmpty = this.passifSections.every(section => section.fields.length === 0);

    if (!this.templateName || (actifEmpty && passifEmpty)) {
      Swal.fire({
        icon: 'warning',
        title: this.translate.instant('templateBuilder.alerts.template.missingFields'),
        text: this.translate.instant('templateBuilder.alerts.template.missingFields'),
        confirmButtonColor: '#f0ad4e'
      });
      return;
    }

    const template = {
      name: this.templateName,
      formStructure: JSON.stringify({
        actif: this.actifSections.map(section => ({
          name: section.name,
          fields: section.fields.map(field => ({ label: field.label }))
        })),
        passif: this.passifSections.map(section => ({
          name: section.name,
          fields: section.fields.map(field => ({ label: field.label }))
        }))
      })
    };

    this.formTemplateService.saveTemplate(template).subscribe({
      next: data => {
        console.log("Template enregistré avec succès", data);

        Swal.fire({
          icon: 'success',
          title: this.translate.instant('templateBuilder.alerts.template.saveSuccess', { name: this.templateName }),
          text: this.translate.instant('templateBuilder.alerts.template.saveSuccess', { name: this.templateName }),
          confirmButtonColor: '#28a745'
        }).then(() => window.location.reload());
      },
      error: err => {
        console.error("Erreur lors de l'enregistrement du template", err);
        Swal.fire({
          icon: 'error',
          title: this.translate.instant('templateBuilder.alerts.template.saveError'),
          text: this.translate.instant('templateBuilder.alerts.template.saveError'),
          confirmButtonColor: '#d33'
        });
      }
    });
  }


  onFileChange(event: any) {
    const file = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e) => {

        this.handleJsonImport(e.target!.result as string);
      };
      reader.readAsText(file);
    }
  }

  handleJsonImport(jsonString: string) {
    try {
      const parsedTemplate = JSON.parse(jsonString);

      if (!parsedTemplate.name || !parsedTemplate.actif || !parsedTemplate.passif) {
        Swal.fire({
          icon: 'warning',
          title: this.translate.instant('templateBuilder.alerts.json.invalidFile'),
          text: this.translate.instant('templateBuilder.alerts.json.invalidFile'),
          confirmButtonColor: '#f0ad4e'
        });
        return;
      }

      this.templateName = parsedTemplate.name;

      this.actifSections = parsedTemplate.actif.map((section: any) => {
        const fields: FinancialField[] = section.fields.map((field: any) => ({
          label: field.label,
          sectionType: 'actif',
          inputType: 'text'
        }));
        return { ...section, fields };
      });

      this.passifSections = parsedTemplate.passif.map((section: any) => {
        const fields: FinancialField[] = section.fields.map((field: any) => ({
          label: field.label,
          sectionType: 'passif',
          inputType: 'text'
        }));
        return { ...section, fields };
      }); Swal.fire({
        icon: 'success',
        title: this.translate.instant('templateBuilder.alerts.json.importSuccess'),
        text: this.translate.instant('templateBuilder.alerts.json.importSuccess'),
        confirmButtonColor: '#28a745'
      });

    } catch (error) {
      console.error("Erreur lors de l'importation du JSON", error); Swal.fire({
        icon: 'error',
        title: this.translate.instant('templateBuilder.alerts.json.importError'),
        text: this.translate.instant('templateBuilder.alerts.json.importError'),
        confirmButtonColor: '#d33'
      });
    }
  }


  handleJsonImportFromText(jsonText: string) {
    if (jsonText.trim()) {
      this.handleJsonImport(jsonText);
    }
  }

  goToTemplates(): void {
    this.router.navigate(['/template-list']);
  }


}
