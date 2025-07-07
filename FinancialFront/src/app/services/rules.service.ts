import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { RuleDto } from '../models/rule-dto.model';

@Injectable({
  providedIn: 'root'
})
export class RulesService {

  private baseUrl = 'http://localhost:8081';
  private dmnCreateUrl = `${this.baseUrl}/dmn/create`;

  constructor(private http: HttpClient) { }

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  /**
   * Crée une nouvelle règle DMN côté backend.
   * @param ruleKey Identifiant unique de la règle DMN
   * @param rules Liste des règles (RuleDto) à convertir en DMN XML
   * @returns Observable avec la réponse du backend
   */
  createDmnRule(ruleKey: string, templateId: number, rules: RuleDto[]): Observable<any> {
    const headers = this.getAuthHeaders();
    const url = `${this.dmnCreateUrl}?ruleKey=${encodeURIComponent(ruleKey)}&templateId=${templateId}`;
    return this.http.post(url, rules, { headers });
  }

  /**
 * Supprime une règle DMN par son ID.
 * @param id Identifiant de la règle DMN à supprimer
 * @returns Observable avec la réponse du backend
 */
  deleteDmnRule(id: number): Observable<any> {
    const headers = this.getAuthHeaders();
    const url = `${this.baseUrl}/dmn/${id}`;
    return this.http.delete(url, { headers });
  }


}
