import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TemplateListService } from '../../services/template-list.service';
import { RulesService } from '../../services/rules.service';
import { RuleDto } from '../../models/rule-dto.model';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import Swal from 'sweetalert2';


interface Rule {
  expression: string;
  operator: string;
  value: string;
  severity: string;
  message: string;
}

interface Template {
  id: number;
  name: string;
  variables: string[];
  formStructure: string;
}

@Component({
  selector: 'app-rule-builder',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './rule-builder.component.html',
  styleUrls: ['./rule-builder.component.css']
})
export class RuleBuilderComponent implements OnInit {

  currentTemplateVariables: string[] = [];
  templates: any[] = [];
  selectedTemplate: number | null = null;
  allVariables: string[] = [];
  functions: string[] = ['+', '-', '*', '/', '(', ')'];
  searchVariable: string = '';
  expressionParts: string[] = [];
  rules: Rule[] = [];

  private expressionHistory: string[][] = [];
  private currentHistoryIndex: number = -1;
  isDraggingOver: boolean = false;

  variables = ['chiffre_affaire', 'charges', 'benefice', 'actif', 'passif'];
  operators = ['+', '-', '*', '/', '(', ')'];
  builtExpressions: string[] = [];

  constructor(
    private templateService: TemplateListService,
    private rulesService: RulesService
  ) { }


  ngOnInit() {
    this.templateService.getAllTemplates().subscribe({
      next: (data) => {
        this.templates = data;
        const dynamicLabels: Set<string> = new Set();
        for (const template of data) {
          try {
            const structure = JSON.parse(template.formStructure);
            const allSections = [...(structure.actif || []), ...(structure.passif || [])];
            for (const section of allSections) {
              for (const field of section.fields || []) {
                if (field.label) {
                  dynamicLabels.add(field.label);
                }
              }
            }
          } catch (e) {
            console.error(`Erreur de parsing du formStructure du template ${template.id}`, e);
          }
        }
        this.allVariables = [...new Set([...this.allVariables, ...Array.from(dynamicLabels)])];
      },
      error: (err) => {
        console.error('Erreur de récupération des templates', err);
      }
    });
  }

  onSaveRules(): void {
    if (this.selectedTemplate === null) {
      Swal.fire('Erreur', 'Veuillez d\'abord sélectionner un template.', 'error');
      return;
    }

    Swal.fire({
      title: 'Nom de la règle',
      input: 'text',
      inputPlaceholder: 'Entrez un nom unique pour la règle',
      showCancelButton: true,
      confirmButtonText: 'Enregistrer',
      cancelButtonText: 'Annuler',
      inputValidator: (value) => {
        if (!value || value.trim().length === 0) {
          return 'Le nom de la règle est obligatoire';
        }
        return null;
      }
    }).then(result => {
      if (result.isConfirmed) {
        const ruleKey = result.value.trim();
        const rulesDto: RuleDto[] = this.rules.map(rule => ({
          expression: rule.expression,
          condition: rule.operator,
          value: rule.value,
          messageErreur: rule.message,
          severite: rule.severity === 'Bloquant' ? 'Blocking' : 'Warning'
        }));

        this.rulesService.createDmnRule(ruleKey, this.selectedTemplate!, rulesDto).subscribe({
          next: () => {
            Swal.fire('Succès', 'Règle enregistrée avec succès', 'success');
          },
          error: (err) => {
            console.error('Erreur lors de la sauvegarde', err);
            Swal.fire('Erreur', 'Impossible d\'enregistrer la règle', 'error');
          }
        });
      }
    });
  }

  filteredVariables(): string[] {
    if (!this.searchVariable) {
      return this.currentTemplateVariables;
    }
    const query = this.searchVariable.toLowerCase();
    return this.currentTemplateVariables.filter(v => v.toLowerCase().includes(query));
  }

  onTemplateChange() {

    this.rules = [];
    this.expressionParts = [];
    this.currentTemplateVariables = [];

    const templateId = typeof this.selectedTemplate === 'string' ? parseInt(this.selectedTemplate, 10) : this.selectedTemplate;

    if (templateId !== null) {
      const selected = this.templates.find(t => t.id === templateId);
      if (selected) {
        try {
          const structure = JSON.parse(selected.formStructure);
          this.currentTemplateVariables = this.extractVariables(structure);

        } catch (e) {
          console.error('Erreur de parsing du formStructure:', e);
          this.currentTemplateVariables = [];
        }
      }
    }
  }

  private extractVariables(structure: any): string[] {

    const variables = new Set<string>();
    const processSection = (section: any) => {
      if (section.fields) {
        section.fields.forEach((field: any) => {
          if (field.label?.trim()) {
            variables.add(field.label.trim());
          }
          if (field.sections) {
            field.sections.forEach(processSection);
          }
        });
      }
      if (section.sections) {
        section.sections.forEach(processSection);
      }
    };
    const mainSectionKeys = ['actif', 'passif', 'sections', 'autresSections', 'content'];
    mainSectionKeys.forEach(key => {
      if (structure[key]) {
        structure[key].forEach(processSection);
      }
    });

    return Array.from(variables);
  }

  allowDrop(event: DragEvent) {
    event.preventDefault();
  }

  onDragStart(event: DragEvent, item: string, type: 'variable' | 'operator') {

    event.dataTransfer?.setData('text/plain', JSON.stringify({ item, type }));
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    const droppedItemData = event.dataTransfer?.getData('text/plain');
    if (droppedItemData) {
      const droppedItem = JSON.parse(droppedItemData);
      if (droppedItem.type === 'variable' || droppedItem.type === 'operator') {
        this.expressionParts.push(droppedItem.item);
        this.saveToHistory();
      }
    }
  }

  addExpression(): void {
    const expression = this.expressionParts.join(' ').trim();

    if (!expression) return;

    if (!this.isExpressionValid(expression)) {
      Swal.fire({
        icon: 'error',
        title: 'Expression invalide',
        text: 'Veuillez vérifier la syntaxe de votre expression avant de l\'ajouter.',
        confirmButtonColor: '#d33'
      });
      return;
    }

    const emptyRule = this.rules.find(rule => !rule.expression);
    if (emptyRule) {
      emptyRule.expression = expression;
    } else {
      this.rules.push({
        expression: expression,
        operator: '>',
        value: '',
        severity: 'Bloquant',
        message: ''
      });
    }

    this.expressionParts = [];
    this.saveToHistory();
  }

  clearExpression(): void {
    this.expressionParts = [];
    this.saveToHistory();
  }

  removeExpressionPart(index: number): void {
    this.saveToHistory();
    this.expressionParts.splice(index, 1);
  }



  removeRule(index: number) {
    this.rules.splice(index, 1);
  }

  createNewRule() {
    const lastRule = this.rules[this.rules.length - 1];

    if (lastRule && !lastRule.expression) {
      Swal.fire({
        icon: 'warning',
        title: 'Règle incomplète',
        text: 'Veuillez d\'abord compléter l\'expression de la dernière règle avant d\'en ajouter une nouvelle.',
        confirmButtonColor: '#3085d6'
      });
      return;
    }

    this.rules.push({
      expression: '',
      operator: '>',
      value: '',
      severity: 'Bloquant',
      message: ''
    });
  }


  isExpressionValid(expression: string): boolean {
    if (!expression || !expression.trim()) return false;

    const allowedOperators = new Set(['+', '-', '*', '/', '(', ')']);
    const allowedVariables = new Set(this.currentTemplateVariables);
    const tokens = expression.split(/\s+/).filter(token => token.length > 0);

    if (tokens.length === 0) return false;

    let i = 0;
    let balance = 0;
    let expectOperand = true; // Start expecting an operand
    let lastTokenType: 'operand' | 'operator' | 'openParen' | 'closeParen' | null = null;

    while (i < tokens.length) {
      let foundVariable = false;
      let variableLength = 0;

      // Try to match multi-word variables (longest match first)
      for (let j = tokens.length; j > i; j--) {
        const candidate = tokens.slice(i, j).join(' ');
        if (allowedVariables.has(candidate)) {
          variableLength = j - i;
          foundVariable = true;
          break;
        }
      }

      if (foundVariable) {
        // Variable found
        if (!expectOperand) {
          return false; // Two operands in a row
        }
        i += variableLength;
        expectOperand = false;
        lastTokenType = 'operand';
        continue;
      }

      const token = tokens[i];

      // Check for numbers
      if (/^\d+(\.\d+)?$/.test(token)) {
        if (!expectOperand) {
          return false; // Two operands in a row
        }
        expectOperand = false;
        lastTokenType = 'operand';
        i++;
        continue;
      }

      // Check for operators
      if (['+', '-', '*', '/'].includes(token)) {
        // Handle unary minus/plus at the beginning or after opening parenthesis
        if ((token === '-' || token === '+') &&
          (i === 0 || lastTokenType === 'openParen' || lastTokenType === 'operator')) {
          if (!expectOperand) {
            return false;
          }
          // Unary operator - still expect operand after it
          lastTokenType = 'operator';
          i++;
          continue;
        }

        if (expectOperand) {
          return false; // Two operators in a row (except unary)
        }
        expectOperand = true;
        lastTokenType = 'operator';
        i++;
        continue;
      }

      // Check for opening parenthesis
      if (token === '(') {
        if (!expectOperand) {
          return false; // Opening paren after operand without operator
        }
        balance++;
        lastTokenType = 'openParen';
        expectOperand = true;
        i++;
        continue;
      }

      // Check for closing parenthesis
      if (token === ')') {
        if (expectOperand) {
          return false; // Empty parentheses or operator before closing paren
        }
        balance--;
        if (balance < 0) {
          return false; // Unmatched closing parenthesis
        }
        lastTokenType = 'closeParen';
        expectOperand = false;
        i++;
        continue;
      }

      // If we reach here, token is invalid
      return false;
    }

    // Final validations
    if (balance !== 0) {
      return false; // Unmatched parentheses
    }

    if (expectOperand) {
      return false; // Expression ends with operator
    }

    // Check for empty expression
    if (tokens.length === 0) {
      return false;
    }

    return true;
  }

  // Optional: Helper function to provide more detailed error messages
  getExpressionValidationError(expression: string): string | null {
    if (!expression || !expression.trim()) {
      return "Expression is empty";
    }

    const allowedOperators = new Set(['+', '-', '*', '/', '(', ')']);
    const allowedVariables = new Set(this.currentTemplateVariables);
    const tokens = expression.split(/\s+/).filter(token => token.length > 0);

    if (tokens.length === 0) {
      return "Expression is empty";
    }

    let i = 0;
    let balance = 0;
    let expectOperand = true;
    let lastTokenType: 'operand' | 'operator' | 'openParen' | 'closeParen' | null = null;

    while (i < tokens.length) {
      let foundVariable = false;
      let variableLength = 0;

      // Try to match multi-word variables
      for (let j = tokens.length; j > i; j--) {
        const candidate = tokens.slice(i, j).join(' ');
        if (allowedVariables.has(candidate)) {
          variableLength = j - i;
          foundVariable = true;
          break;
        }
      }

      if (foundVariable) {
        if (!expectOperand) {
          return `Two operands in a row at position ${i}: missing operator between values`;
        }
        i += variableLength;
        expectOperand = false;
        lastTokenType = 'operand';
        continue;
      }

      const token = tokens[i];

      if (/^\d+(\.\d+)?$/.test(token)) {
        if (!expectOperand) {
          return `Two operands in a row at position ${i}: missing operator between '${tokens[i - 1]}' and '${token}'`;
        }
        expectOperand = false;
        lastTokenType = 'operand';
        i++;
        continue;
      }

      if (['+', '-', '*', '/'].includes(token)) {
        if ((token === '-' || token === '+') &&
          (i === 0 || lastTokenType === 'openParen' || lastTokenType === 'operator')) {
          lastTokenType = 'operator';
          i++;
          continue;
        }

        if (expectOperand) {
          return `Two operators in a row at position ${i}: '${token}'`;
        }
        expectOperand = true;
        lastTokenType = 'operator';
        i++;
        continue;
      }

      if (token === '(') {
        if (!expectOperand) {
          return `Missing operator before opening parenthesis at position ${i}`;
        }
        balance++;
        lastTokenType = 'openParen';
        expectOperand = true;
        i++;
        continue;
      }

      if (token === ')') {
        if (expectOperand) {
          return `Empty parentheses or operator before closing parenthesis at position ${i}`;
        }
        balance--;
        if (balance < 0) {
          return `Unmatched closing parenthesis at position ${i}`;
        }
        lastTokenType = 'closeParen';
        expectOperand = false;
        i++;
        continue;
      }

      return `Invalid token '${token}' at position ${i}`;
    }

    if (balance !== 0) {
      return balance > 0 ? "Unmatched opening parenthesis" : "Unmatched closing parenthesis";
    }

    if (expectOperand) {
      return "Expression ends with an operator";
    }

    return null;
  }

  toggleSeverity(rule: any): void {
    rule.severity = rule.severity === 'Bloquant' ? 'Avertissement' : 'Bloquant';
  }



  saveToHistory(): void {
    // On ne garde que l'historique jusqu'à l'index courant
    this.expressionHistory = this.expressionHistory.slice(0, this.currentHistoryIndex + 1);
    this.expressionHistory.push([...this.expressionParts]);
    this.currentHistoryIndex = this.expressionHistory.length - 1;
  }

  undo(): void {
    if (this.canUndo) {
      this.currentHistoryIndex--;
      this.expressionParts = [...this.expressionHistory[this.currentHistoryIndex]];
    }
  }

  redo(): void {
    if (this.canRedo) {
      this.currentHistoryIndex++;
      this.expressionParts = [...this.expressionHistory[this.currentHistoryIndex]];
    }
  }

  get canUndo(): boolean {
    return this.currentHistoryIndex > 0;
  }

  get canRedo(): boolean {
    return this.currentHistoryIndex < this.expressionHistory.length - 1;
  }

  // Fonction de copie
  copyExpression(): void {
    if (this.expressionParts.length === 0) return;

    const expression = this.expressionParts.join(' ');
    navigator.clipboard.writeText(expression).then(() => {
      Swal.fire({
        icon: 'success',
        title: 'Expression copiée',
        text: 'L\'expression a été copiée dans le presse-papier',
        timer: 2000,
        showConfirmButton: false
      });
    });
  }


}