import { Component, OnInit, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { TemplateListService } from '../../services/template-list.service';
import { Router } from '@angular/router';
import { ErrorMessagesService } from '../../services/error-handler.service';
import { ReportService } from '../../services/report.service';
import { RulesService } from '../../services/rules.service';
import { Modal } from 'bootstrap';
import { SafePipe } from '../../pipes/safe.pipe';
import { ExpressionEvaluationResult } from '../../models/expression-evaluation-result.model';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import * as XLSX from 'xlsx';
import * as FileSaver from 'file-saver';
import * as ExcelJS from 'exceljs';

import Swal from 'sweetalert2';


interface Field {
  label: string;
}

interface Section {
  name: string;
  fields: Field[];
}

interface Template {
  id?: number;
  name?: string;
  designName?: string;
  previewImageUrl?: string;
  actif?: Section[];
  passif?: Section[];
  dateCreation?: string;
}

@Component({
  selector: 'app-template-list',
  imports: [CommonModule, ReactiveFormsModule, SafePipe, FormsModule, TranslateModule],
  templateUrl: './template-list.component.html',
  styleUrls: ['./template-list.component.css'],
  standalone: true
})

export class TemplateListComponent implements OnInit {
  templates: Template[] = [];
  selectedTemplate: Template | null = null;
  selectedDmn: any | null = null;
  selectedDmnDisplay: any | null = null;
  form: FormGroup = this.fb.group({});

  compatibleDmns: any[] = [];

  isDmnListVisible: boolean = false;
  isLoadingDmns: boolean = false;
  isDesignContainerVisible: boolean = false;
  isDesignModalVisible = false;
  isLoadingDesigns = false;

  previewReport: string | null = null;
  previewError: string | null = null;

  currentPage: number = 1;
  itemsPerPage: number = 5;


  hoverMessage: string = '';
  hoverX: number = 0;
  hoverY: number = 0;

  isPreviewModalVisible = false;
  previewedRules: any[] = [];


  @ViewChild('dmnFileInput') dmnFileInput!: ElementRef;
  @ViewChild('templateForm') templateForm!: ElementRef;
  @ViewChild('previewModalRef', { static: false }) previewModalRef!: ElementRef;
  previewModal!: Modal;

  evaluationResults!: ExpressionEvaluationResult[];

  activeTab: string = 'preview';
  openSections: { [key: string]: boolean } = {
    errors: true,
    warnings: true,
    success: true
  };

  constructor(
    private templateListService: TemplateListService,
    private fb: FormBuilder,
    private router: Router,
    private errorMessagesService: ErrorMessagesService,
    private reportService: ReportService,
    private cdr: ChangeDetectorRef,
    private translate: TranslateService,
    private rulesService: RulesService
  ) {
    this.evaluationResults = [];
  }

  ngOnInit(): void {
    this.loadTemplates();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      if (this.previewModalRef && this.previewModalRef.nativeElement) {
        this.previewModal = new Modal(this.previewModalRef.nativeElement);
        console.log("Modal initialisé avec succès");
      } else {
        console.error("Référence modale non disponible");
      }
    });
  }

  closePreview(): void {
    if (this.previewModal) {
      this.previewModal.hide();
    }
  }


  loadTemplates(): void {
    this.templateListService.getAllTemplates().subscribe({
      next: (data: any[]) => {
        console.log("Templates reçus du backend :", data);
        this.templates = data.map((template, index) => {
          const parsedTemplate = JSON.parse(template.formStructure);
          return {
            ...template,
            previewImageUrl: [
              //"assets/img/brand/school-championship-form.jpg",
              "assets/img/brand/template.png",
              "assets/img/brand/template.png",
              "assets/img/brand/template.png",
              "assets/img/brand/template.png",
            ][index % 4],
            actif: (parsedTemplate.actif as Section[])?.filter((section: Section) => section.fields.length > 0) || [],
            passif: (parsedTemplate.passif as Section[])?.filter((section: Section) => section.fields.length > 0) || []
          };
        });
        console.log("Templates après filtrage des sections vides :", this.templates);
      },
      error: error => {
        console.error('Erreur lors de la récupération des templates', error);
      }
    });
  }

  selectTemplate(template: Template): void {
    this.selectedTemplate = template;
    if (template) {
      this.buildForm(template);
      setTimeout(() => {
        this.templateForm.nativeElement.scrollIntoView({
          behavior: 'smooth',
          block: 'start'
        });
      }, 100);
    }
  }

  buildForm(template: Template): void {
    const group: { [key: string]: any } = {};

    group['companyName'] = ['', Validators.required];

    template.actif?.forEach(section => {
      section.fields.forEach(field => {
        group[this.sanitizeLabel(field.label)] = ['', [Validators.required, Validators.min(0)]];
      });
    });
    template.passif?.forEach(section => {
      section.fields.forEach(field => {
        group[this.sanitizeLabel(field.label)] = ['', [Validators.required, Validators.min(0)]];
      });
    });
    this.form = this.fb.group(group);
  }

  sanitizeLabel(label: string): string {
    return label.replace(/\s+/g, '_');
  }

  onSubmit(): void {
    if (this.form.valid && this.selectedTemplate && this.selectedDmnDisplay) {
      const formData = {
        companyName: this.form.get('companyName')?.value,
        actif: this.selectedTemplate.actif?.map(section => ({
          name: section.name,
          fields: section.fields.map(field => ({
            label: field.label,
            value: Number(this.form.get(this.sanitizeLabel(field.label))?.value || 0)
          }))
        })) || [],
        passif: this.selectedTemplate.passif?.map(section => ({
          name: section.name,
          fields: section.fields.map(field => ({
            label: field.label,
            value: Number(this.form.get(this.sanitizeLabel(field.label))?.value || 0)
          }))
        })) || []
      };

      this.templateListService.submitFinancialStatement(
        formData,
        this.selectedDmnDisplay.ruleKey,
        this.selectedTemplate.designName || 'Design1'
      ).subscribe({
        next: (evaluationResults: ExpressionEvaluationResult[]) => {
          this.evaluationResults = evaluationResults;

          // Check for blocking errors
          const hasBlockingError = evaluationResults.some(
            result => result.severite?.toLowerCase().trim() === 'bloquant' && result.result === true
          );
          if (hasBlockingError) {
            const errorMessages = evaluationResults
              .filter(result => result.severite?.toLowerCase().trim() === 'bloquant')
              .map(result => result.messageErreur)
              .join('\n');

            Swal.fire({
              icon: 'error',
              title: 'Erreurs bloquantes détectées',
              text: errorMessages,
              confirmButtonText: 'OK'
            });
            return;
          }

          // Show warnings if any
          const warnings = evaluationResults.filter(
            result => result.severite.toLowerCase() === 'avertissement'
          );

          if (warnings.length > 0) {
            const warningMessages = warnings
              .map(warning => warning.messageErreur)
              .join('\n');

            Swal.fire({
              icon: 'warning',
              title: 'Avertissements',
              text: warningMessages,
              showCancelButton: true,
              confirmButtonText: 'Continuer',
              cancelButtonText: 'Annuler'
            }).then((result) => {
              if (result.isConfirmed) {
                this.closeTemplateModal();
                Swal.fire('Succès', 'Le rapport financier a été soumis avec succès', 'success');
              }
            });
          } else {
            // No warnings, just success
            this.closeTemplateModal();
            Swal.fire('Succès', 'Le rapport financier a été soumis avec succès', 'success');
          }
        },
        error: (error) => {
          console.error('Erreur lors de la soumission:', error);
          Swal.fire({
            icon: 'error',
            title: 'Erreur',
            text: 'Une erreur est survenue lors de la soumission du rapport'
          });
        }
      });
    } else {
      Swal.fire({
        icon: 'error',
        title: 'Formulaire invalide',
        text: 'Veuillez remplir tous les champs requis et sélectionner un DMN'
      });
    }
  }


  deleteTemplate(templateId: number | undefined): void {
    if (templateId === undefined) {
      return;
    }

    Swal.fire({
      title: 'Confirmation',
      text: 'Voulez-vous vraiment supprimer ce template ?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Oui, supprimer',
      cancelButtonText: 'Annuler'
    }).then((result) => {
      if (result.isConfirmed) {
        this.templateListService.deleteTemplate(templateId).subscribe({
          next: () => {
            this.templates = this.templates.filter(template => template.id !== templateId);
            Swal.fire({
              icon: 'success',
              title: 'Supprimé',
              text: 'Le template a été supprimé avec succès.'
            });
          },
          error: error => {
            console.error('Erreur lors de la suppression du template', error);
            Swal.fire({
              icon: 'error',
              title: 'Erreur',
              text: 'Une erreur est survenue lors de la suppression du template.'
            });
          }
        });
      }
    });
  }

  goToTemplateBuilder(): void {
    this.router.navigate(['/template-builder']);
  }

  triggerDmnFileInput(): void {
    this.dmnFileInput.nativeElement.click();
  }

  onHover(buttonType: string): void {
    if (buttonType === 'static') {
      this.hoverMessage = 'Idéal pour les DMN simples';
    } else if (buttonType === 'ai') {
      this.hoverMessage = 'Idéal pour les DMN complexes';
    } else {
      this.hoverMessage = '';
    }

  }

  onMouseMove(event: MouseEvent): void {

    this.hoverX = event.clientX + 10;
    this.hoverY = event.clientY + 10;

  }

  onDmnFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      const ruleKey = file.name.replace('.dmn', '');

      this.templateListService.importDmn(ruleKey, file).subscribe({
        next: () => {
          Swal.fire({
            title: 'Succès',
            text: 'DMN importé avec succès.',
            icon: 'success'
          });
        },
        error: (error) => {
          console.error("Erreur lors de l'import du DMN", error);
          const errorMessage = error?.error?.error || 'Erreur inconnue.';
          Swal.fire({
            title: 'Erreur',
            text: errorMessage,
            icon: 'error'
          });
        }
      });

      event.target.value = '';
    }
  }


  toggleDmnList(): void {
    this.isDmnListVisible = !this.isDmnListVisible;
  }

  selectCompatibleDmn(method: 'ai' | 'static'): void {
    if (!this.selectedTemplate) {
      Swal.fire({
        icon: 'warning',
        title: 'Template non sélectionné',
        text: 'Veuillez sélectionner un template.',
        confirmButtonText: 'OK',
      });
      return;
    }

    this.isLoadingDmns = true;

    if (method === 'static') {
      // Appeler le nouveau service pour récupérer les DMNs liés à l'ID du template
      this.templateListService.getDmnRulesByTemplateId(this.selectedTemplate.id!).subscribe({
        next: (dmns) => {
          this.compatibleDmns = dmns;
          this.isLoadingDmns = false;
          this.currentPage = 1;
          this.isDmnListVisible = true;
        },
        error: (error) => {
          this.isLoadingDmns = false;
          Swal.fire({
            icon: 'error',
            title: 'Erreur',
            text: 'Erreur lors de la récupération des DMN associés au template.',
            confirmButtonText: 'OK',
          });
        }
      });

    } else {
      // ancien comportement AI
      let fields: string[] = [];
      this.selectedTemplate.actif?.forEach(section => {
        section.fields.forEach(field => fields.push(this.sanitizeLabel(field.label)));
      });
      this.selectedTemplate.passif?.forEach(section => {
        section.fields.forEach(field => fields.push(this.sanitizeLabel(field.label)));
      });

      const fieldsParam = fields.join(",");

      this.templateListService.getCompatibleDmns(fieldsParam, method).subscribe({
        next: (compatibleDmns) => {
          this.compatibleDmns = compatibleDmns;
          this.isDmnListVisible = true;
          this.isLoadingDmns = false;
        },
        error: (error) => {
          this.isLoadingDmns = false;
          Swal.fire({
            icon: 'error',
            title: 'Erreur',
            text: 'Erreur lors de la récupération des DMN compatibles.',
            confirmButtonText: 'OK',
          });
        }
      });
    }
  }

  selectDmnFromList(dmn: any): void {
    this.selectedDmn = dmn;
    this.selectedDmnDisplay = dmn;
    this.isDmnListVisible = false;

    console.log('DMN sélectionné:', dmn.ruleKey);

  }

  closeModal(): void {
    this.isDmnListVisible = false;
  }

  chooseTemplateDesign(): void {
    this.isDesignModalVisible = true;
  }

  /*selectDesign(design: string): void {
    if (this.selectedTemplate) {
      this.selectedTemplate.designName = design;
      this.closeDesignModal();
    } else {
      console.error('selectedTemplate is null or undefined');
    }
  }*/

  closeDesignModal(): void {
    this.isDesignModalVisible = false;
  }

  getDesignImage(design: string): string {
    switch (design) {
      case 'Design1':
        return 'assets/img/design1.png';
      case 'Design2':
        return 'assets/img/design2.png';
      case 'Design3':
        return 'assets/img/design3.png';
      default:
        return 'assets/images/design1.png';
    }
  }

  onPreview(): void {
    console.log("Début de la prévisualisation...");

    if (!this.previewModalRef || !this.previewModalRef.nativeElement) {
      console.error("Référence modale non disponible lors de la prévisualisation");
      Swal.fire({
        icon: 'error',
        title: 'Erreur technique',
        text: 'La modale n\'est pas disponible.',
        confirmButtonText: 'OK',
      });
      return;
    }

    // Réinitialisation au cas où la modale n'est pas encore initialisée
    if (!this.previewModal) {
      console.log("Réinitialisation de la modale car non définie");
      try {
        this.previewModal = new Modal(this.previewModalRef.nativeElement);
      } catch (err) {
        console.error("Erreur lors de la réinitialisation de la modale:", err);
        Swal.fire({
          icon: 'error',
          title: 'Erreur technique',
          text: 'Impossible d\'initialiser la modale.',
          confirmButtonText: 'OK',
        });
        return;
      }
    }

    this.previewReport = null;
    this.previewError = null;

    if (this.form.valid && this.selectedTemplate) {
      console.log("Formulaire valide, calcul des totaux...");
      this.selectedTemplate.designName = 'Design1';

      let totalActif = 0;
      let totalPassif = 0;

      // Calcul des totaux 
      this.selectedTemplate.actif?.forEach(section => {
        section.fields.forEach(field => {
          const fieldValue = this.form.get(this.sanitizeLabel(field.label))?.value || 0;
          totalActif += Number(fieldValue);
        });
      });

      this.selectedTemplate.passif?.forEach(section => {
        section.fields.forEach(field => {
          const fieldValue = this.form.get(this.sanitizeLabel(field.label))?.value || 0;
          totalPassif += Number(fieldValue);
        });
      });

      totalActif = Math.round(totalActif);
      totalPassif = Math.round(totalPassif);

      console.log(`Total actif: ${totalActif}, Total passif: ${totalPassif}`);

      if (totalActif !== totalPassif) {
        this.previewError = 'Le total des passifs doit être égal au total des actifs.';
        console.log("Affichage du modal avec erreur de totaux...");
        this.previewModal.show();
        return;
      }

      const formData = {
        companyName: this.form.get('companyName')?.value,
        actif: this.selectedTemplate.actif?.map(section => ({
          name: section.name,
          fields: section.fields.map(field => ({
            label: field.label,
            value: Number(this.form.get(this.sanitizeLabel(field.label))?.value || 0)
          }))
        })) || [],
        passif: this.selectedTemplate.passif?.map(section => ({
          name: section.name,
          fields: section.fields.map(field => ({
            label: field.label,
            value: Number(this.form.get(this.sanitizeLabel(field.label))?.value || 0)
          }))
        })) || []
      };

      if (this.selectedDmnDisplay) {
        this.templateListService.previewFinancialStatement(
          formData,
          this.selectedDmnDisplay.ruleKey,
          this.selectedTemplate.designName || 'Design1'
        ).subscribe({
          next: (evaluationResults: ExpressionEvaluationResult[]) => {
            this.evaluationResults = evaluationResults || [];
            const hasBlockingError = this.hasBlockingErrors;

            if (hasBlockingError) {
              this.previewError = 'Prévisualisation impossible, il existe des erreurs bloquantes à fixer, consultez le rapport de validation.';
              this.previewModal.show();
              return;
            }

            this.reportService.generateFinancialReport(formData, this.selectedTemplate?.designName ?? '')
              .subscribe({
                next: (pdfBlob: Blob) => {
                  console.log("PDF généré avec succès, taille:", pdfBlob.size, "type:", pdfBlob.type);
                  const fileURL = URL.createObjectURL(pdfBlob) + `#${new Date().getTime()}`;
                  this.previewReport = fileURL;
                  this.previewModal.show();
                },
                error: (error) => {
                  console.error("Erreur lors de la génération du PDF:", error);
                  this.previewError = "Erreur lors de la génération de l'aperçu du rapport.";
                  this.previewModal.show();
                }
              });
          },
          error: (error) => {
            console.error("Erreur lors de l'évaluation DMN (prévisualisation):", error);
            this.previewError = "Erreur lors de l'évaluation des règles DMN.";
            this.evaluationResults = [];
            this.previewModal.show();
          }
        });
      }
      else {
        // If no DMN is selected, just generate preview
        this.reportService.generateFinancialReport(formData, this.selectedTemplate?.designName ?? '')
          .subscribe({
            next: (pdfBlob: Blob) => {
              console.log("PDF généré avec succès, taille:", pdfBlob.size, "type:", pdfBlob.type);
              const fileURL = URL.createObjectURL(pdfBlob) + `#${new Date().getTime()}`;
              this.previewReport = fileURL;
              this.previewModal.show();
            },
            error: (error) => {
              console.error("Erreur lors de la génération du PDF:", error);
              this.previewError = "Erreur lors de la génération de l'aperçu du rapport.";
              this.previewModal.show();
            }
          });
      }
    } else {
      console.log("Formulaire invalide ou templates non sélectionnés");
      this.previewError = this.translate.instant('templateList.modal.validationErrors.previewError');
      this.previewModal.show();
    }
  }

  isValidationError(error: string | null): boolean {
    if (!error) return false;
    const cleaned = error.trim();
    return cleaned === 'Veuillez remplir tous les champs, sélectionner un modèle et un DMN.' ||
      cleaned === 'Erreur : Veuillez remplir tous les champs, sélectionner un modèle et un DMN.';
  }

  openTemplateModal(template: Template) {
    this.selectTemplate(template);
  }

  closeTemplateModal() {
    this.selectedTemplate = null;
  }

  detachDmn() {
    this.selectedDmnDisplay = null;
  }


  loadDmnsForTemplate(templateId: number): void {
    this.isLoadingDmns = true;
    this.templateListService.getDmnRulesByTemplateId(templateId).subscribe({
      next: (dmns) => {
        this.compatibleDmns = dmns;
        this.currentPage = 1;
        this.isDmnListVisible = true;
        this.isLoadingDmns = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement des DMN :', err);
        this.isLoadingDmns = false;
      }
    });
  }


  get totalPages(): number {
    return Math.ceil(this.compatibleDmns.length / this.itemsPerPage);
  }

  paginatedDmns() {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.compatibleDmns.slice(start, start + this.itemsPerPage);
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }

  prevPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
    }

  }

  searchTerm: string = '';
  get filteredTemplates(): Template[] {
    return this.templates.filter(template =>
      template.name?.toLowerCase().includes(this.searchTerm.toLowerCase())
    );
  }

  get blockingErrors(): ExpressionEvaluationResult[] {
    return this.evaluationResults.filter(
      result =>
        ['blocking', 'bloquant'].includes(result.severite?.toLowerCase() || '') &&
        result.result === true
    );
  }

  get warnings(): ExpressionEvaluationResult[] {
    return this.evaluationResults.filter(
      result =>
        ['warning', 'avertissement'].includes(result.severite?.toLowerCase() || '') &&
        result.result === true
    );
  }

  get successValidations(): ExpressionEvaluationResult[] {
    return this.evaluationResults.filter(
      result =>
        !['bloquant', 'blocking', 'warning', 'avertissement'].includes(result.severite?.toLowerCase() || '') ||
        result.result === false
    );
  }


  get hasBlockingErrors(): boolean {
    return this.blockingErrors.length > 0;
  }

  switchTab(tab: string): void {
    console.log('Switching to tab:', tab);
    this.activeTab = tab;
    this.cdr.detectChanges();
  }

  toggleSection(section: string) {
    if (section in this.openSections) {
      this.openSections[section] = !this.openSections[section];
    }
  }

  exportValidationReport(): void {
    if (this.evaluationResults.length === 0) {
      return;
    }

    // Format du rapport
    const reportData = {
      date: new Date().toLocaleString(),
      companyName: this.form.get('companyName')?.value || 'N/A',
      summary: {
        total: this.evaluationResults.length,
        errors: this.blockingErrors.length,
        warnings: this.warnings.length,
        success: this.successValidations.length,
        validationRate: ((this.successValidations.length / this.evaluationResults.length) * 100).toFixed(0) + '%'
      },
      details: {
        errors: this.blockingErrors,
        warnings: this.warnings,
        success: this.successValidations
      }
    };

    // Convertir en JSON
    const jsonString = JSON.stringify(reportData, null, 2);
    const blob = new Blob([jsonString], { type: 'application/json' });

    // Créer un lien de téléchargement
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `validation-report-${new Date().toISOString().slice(0, 10)}.json`;
    document.body.appendChild(a);
    a.click();

    // Nettoyer
    setTimeout(() => {
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    }, 0);

    // Afficher une notification
    Swal.fire({
      icon: 'success',
      title: 'Rapport exporté',
      text: 'Le rapport de validation a été téléchargé',
      toast: true,
      position: 'top-end',
      showConfirmButton: false,
      timer: 3000
    });
  }

  openPreviewModal(dmn: any): void {
    if (dmn.ruleDtosJson) {
      try {
        console.log("DMN sélectionné :", dmn);
        this.previewedRules = JSON.parse(dmn.ruleDtosJson);
        this.isPreviewModalVisible = true;
      } catch (e) {
        console.error("Erreur parsing JSON des règles DMN", e);
        this.previewedRules = [];
      }
    }
  }

  closePreviewModal(): void {
    this.isPreviewModalVisible = false;
    this.previewedRules = [];
  }

  // D'abord, installez la dépendance : npm install xlsx

  async exportToExcel(): Promise<void> {
    interface ValidationRow {
      Type: string;
      Expression: string;
      Condition: string;
      Message: string;
      ValeurÉvaluée: string;
    }

    const rows: ValidationRow[] = [];

    const addRows = (list: any[], type: string) => {
      list.forEach(item => {
        rows.push({
          Type: type,
          Expression: item.expression,
          Condition: item.condition,
          Message: item.messageErreur || '',
          ValeurÉvaluée: item.evaluatedValue || ''
        });
      });
    };

    addRows(this.blockingErrors, 'reject');
    addRows(this.warnings, 'avertissement');
    addRows(this.successValidations, 'succes');

    // Create workbook and worksheet
    const workbook = new ExcelJS.Workbook();
    const worksheet = workbook.addWorksheet('Validation Report');

    // Add headers
    worksheet.addRow(['Type', 'Expression', 'Condition', 'Message', 'Valeur Évaluée']);

    // Style header row
    const headerRow = worksheet.getRow(1);
    headerRow.eachCell((cell) => {
      cell.fill = {
        type: 'pattern',
        pattern: 'solid',
        fgColor: { argb: 'FFD3D3D3' }
      };
      cell.font = { bold: true };
      cell.border = {
        top: { style: 'thin' },
        left: { style: 'thin' },
        bottom: { style: 'thin' },
        right: { style: 'thin' }
      };
    });

    // Define row colors
    const rowColors: { [key: string]: string } = {
      'succes': 'FF90EE90',      // Light green
      'avertissement': 'FFFFFF00', // Yellow
      'reject': 'FFFF6B6B'       // Light red
    };

    // Add data rows with styling
    rows.forEach((row) => {
      const excelRow = worksheet.addRow([
        row.Type,
        row.Expression,
        row.Condition,
        row.Message,
        row.ValeurÉvaluée
      ]);

      // Apply row coloring based on type
      const color = rowColors[row.Type.toLowerCase()];
      if (color) {
        excelRow.eachCell((cell) => {
          cell.fill = {
            type: 'pattern',
            pattern: 'solid',
            fgColor: { argb: color }
          };
          cell.border = {
            top: { style: 'thin' },
            left: { style: 'thin' },
            bottom: { style: 'thin' },
            right: { style: 'thin' }
          };
        });
      }
    });

    // Set column widths
    worksheet.columns = [
      { width: 10 }, // Type
      { width: 80 }, // Expression
      { width: 20 }, // Condition
      { width: 50 }, // Message
      { width: 20 }  // Valeur Évaluée
    ];

    // Generate Excel file
    const buffer = await workbook.xlsx.writeBuffer();
    const blob = new Blob([buffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    });

    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'validation_report.xlsx');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }

  deleteDmn(dmn: any): void {
    Swal.fire({
      title: this.translate.instant('templateList.modal.deleteDmnTitle'),
      text: this.translate.instant('templateList.modal.deleteDmnText', { ruleKey: dmn.ruleKey }),
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: this.translate.instant('templateList.modal.confirmDelete'),
      cancelButtonText: this.translate.instant('templateList.modal.cancelDelete')
    }).then((result) => {
      if (result.isConfirmed) {
        this.isLoadingDmns = true;
        this.rulesService.deleteDmnRule(dmn.id).subscribe({
          next: () => {
            this.compatibleDmns = this.compatibleDmns.filter(item => item.id !== dmn.id);
            this.isLoadingDmns = false;
            Swal.fire({
              icon: 'success',
              title: this.translate.instant('templateList.modal.deleteDmnSuccess'),
              text: this.translate.instant('templateList.modal.deleteDmnSuccessText', { ruleKey: dmn.ruleKey }),
              timer: 2000,
              showConfirmButton: false
            });
          },
          error: (error) => {
            this.isLoadingDmns = false;
            Swal.fire({
              icon: 'error',
              title: this.translate.instant('templateList.modal.deleteDmnError'),
              text: this.translate.instant('templateList.modal.deleteDmnErrorText'),
            });
          }
        });
      }
    });
  }

}
