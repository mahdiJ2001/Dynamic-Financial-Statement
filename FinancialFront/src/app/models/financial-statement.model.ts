import { User } from './user.model';

export class FinancialStatement {
  id: number;
  formData: string;
  report: string | null;
  createdAt: string;
  createdBy: User;

  constructor(id: number, formData: string, report: string | null, createdAt: string, createdBy: User) {
    this.id = id;
    this.formData = formData;
    this.report = report;
    this.createdAt = createdAt;
    this.createdBy = createdBy;
  }
}