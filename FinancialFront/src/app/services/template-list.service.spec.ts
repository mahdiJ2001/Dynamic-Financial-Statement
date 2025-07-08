import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TemplateListService {
  // URL de l'API pour récupérer les templates
  private apiUrl = 'http://localhost:8081/api/form-templates';

  constructor(private http: HttpClient) { }

  // Récupérer tous les templates
  getAllTemplates(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }
}
