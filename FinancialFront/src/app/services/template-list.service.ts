import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ExpressionEvaluationResult } from '../models/expression-evaluation-result.model';


@Injectable({
  providedIn: 'root'
})
export class TemplateListService {
  private apiUrl = 'http://localhost:8081/api/form-templates';
  private financialStatementsUrl = 'http://localhost:8081/api/financial-statements';
  private dmnUrl = 'http://localhost:8081/dmn';
  private dmnImportUrl = 'http://localhost:8081/dmn/import';
  private dmnCompatibleUrlAI = 'http://localhost:8081/dmn/compatible/ai';
  private dmnCompatibleUrlStatic = 'http://localhost:8081/dmn/compatible/static';

  constructor(private http: HttpClient) { }

  // Helper function to get the JWT token
  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  getAllTemplates(): Observable<any[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<any[]>(this.apiUrl, { headers });
  }

  deleteTemplate(templateId: number): Observable<any> {
    const headers = this.getAuthHeaders();
    return this.http.delete(`${this.apiUrl}/${templateId}`, { headers });
  }


  submitFinancialStatement(
    formData: any,
    ruleKey: string,
    designName: string
  ): Observable<ExpressionEvaluationResult[]> {
    const encodedRuleKey = encodeURIComponent(ruleKey);
    const encodedDesignName = encodeURIComponent(designName);
    const headers = this.getAuthHeaders();

    const requestBody = {
      formData: JSON.stringify(formData)
    };

    return this.http.post<ExpressionEvaluationResult[]>(
      `${this.financialStatementsUrl}?ruleKey=${encodedRuleKey}&designName=${encodedDesignName}`,
      requestBody,
      { headers }
    );
  }

  previewFinancialStatement(
    formData: any,
    ruleKey: string,
    designName: string
  ): Observable<ExpressionEvaluationResult[]> {
    const encodedRuleKey = encodeURIComponent(ruleKey);
    const encodedDesignName = encodeURIComponent(designName);
    const headers = this.getAuthHeaders();

    const requestBody = {
      formData: JSON.stringify(formData)
    };

    return this.http.post<ExpressionEvaluationResult[]>(
      `${this.financialStatementsUrl}/preview?ruleKey=${encodedRuleKey}&designName=${encodedDesignName}`,
      requestBody,
      { headers }
    );
  }



  importDmn(ruleKey: string, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('ruleKey', ruleKey);
    formData.append('file', file);


    let headers = this.getAuthHeaders();
    headers = headers.delete('Content-Type');

    return this.http.post<any>(this.dmnImportUrl, formData, { headers });
  }

  getCompatibleDmns(fields: string, method: 'ai' | 'static'): Observable<any> {
    const encodedFields = encodeURIComponent(fields);
    const headers = this.getAuthHeaders();

    if (method === 'ai') {
      return this.http.get<any>(`${this.dmnCompatibleUrlAI}?fields=${encodedFields}`, { headers });
    } else {
      return this.http.get<any>(`${this.dmnCompatibleUrlStatic}?fields=${encodedFields}`, { headers });
    }
  }

  getAllFinancialStatements(): Observable<any[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<any[]>(this.financialStatementsUrl, { headers });
  }

  getAllDmnRules(): Observable<any[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<any[]>(this.dmnUrl, { headers });
  }
  getDmnRulesByTemplateId(templateId: number): Observable<any[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<any[]>(`${this.dmnUrl}/by-template/${templateId}`, { headers });
  }


  updateStatus(id: number, status: string, rejectionCause?: string): Observable<any> {
    const headers = this.getAuthHeaders();

    let url = `${this.financialStatementsUrl}/status/${id}?status=${status}`;

    if (rejectionCause) {
      url += `&rejectionCause=${encodeURIComponent(rejectionCause)}`;
    }

    return this.http.put<any>(url, null, { headers });
  }

  /**
 * Get chat messages for a financial statement
 */
  getChatMessages(financialStatementId: string): Observable<any[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<any[]>(
      `${this.financialStatementsUrl}/${financialStatementId}/messages`,
      { headers }
    );
  }

  /**
   * Send a chat message
   */
  sendChatMessage(messageData: {
    financialStatementId: string;
    senderId: number;
    senderRole: string;
    content: string;
    timestamp: Date;
  }): Observable<any> {
    const headers = this.getAuthHeaders().set('Content-Type', 'text/plain');
    return this.http.post<any>(
      `${this.financialStatementsUrl}/${messageData.financialStatementId}/messages?senderId=${messageData.senderId}`,
      messageData.content,
      { headers }
    );
  }


}
