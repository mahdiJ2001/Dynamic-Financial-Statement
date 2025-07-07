export interface RuleDto {
    expression: string;
    condition: string;
    value: string;
    messageErreur: string;
    severite: 'Warning' | 'Blocking';
}
